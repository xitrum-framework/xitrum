package xitrum.util

import java.net.URLEncoder

import scala.collection.mutable.{Map => MMap}
import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.control.NonFatal

import akka.actor.{Actor, ActorRef, Props, Identify, ActorIdentity}
import akka.pattern.ask

import com.hazelcast.core.{IMap, MembershipEvent, MembershipListener}

import xitrum.{Config, Logger}

/**
 * This feature is implemented using Akka Remoting and Hazelcast distributed lock
 * (blocking).
 *
 * You must config config/akka.conf to use Akka Remoting:
 * http://doc.akka.io/docs/akka/2.2.0/scala/remoting.html
 *
 * With Akka Cluster Singleton Pattern, this feature may be removed in the future:
 * http://doc.akka.io/docs/akka/2.2.0/contrib/cluster-singleton.html
 * https://groups.google.com/group/akka-user/browse_thread/thread/23d6b2851648c1b0
 *
 * To lookup, from your actor call
 * ActorRegistry.actorRef ! Lookup(name)
 * then receive Option[ActorRef].
 *
 * To lookup and create if there's no actor, from your actor call
 * ActorRegistry.actorRef ! LookupOrCreate(name, propsMaker)
 * then receive tuple (newlyCreated, actorRef). propsMaker is used to create the
 * actor if it does not exist.
 */
object ActorRegistry extends Logger {
  case class Lookup(name: String)
  case class LookupOrCreate(name: String, propsMaker: () => Props)

  //----------------------------------------------------------------------------

  val actorRef = {
    if (Config.application.getString("akka.actor.provider") == "akka.remote.RemoteActorRefProvider")
      Config.actorSystem.actorOf(Props[RemoteActorRegistry], RemoteActorRegistry.ACTOR_NAME)
    else
      Config.actorSystem.actorOf(Props[LocalActorRegistry], LocalActorRegistry.ACTOR_NAME)
  }

  /** Should be called at application start. */
  def start() {
    logger.info("ActorRegistry started: " + actorRef)
  }

  def escape(name: String) = URLEncoder.encode(name, "UTF-8")
}

object RemoteActorRegistry {
  val ACTOR_NAME = ActorRegistry.escape(getClass.getName)

  private case class LookupLocal(name: String)
  private val LOOKUP_TIMEOUT = 5.seconds
}

object LocalActorRegistry {
  val ACTOR_NAME = ActorRegistry.escape(getClass.getName)

  private case class IdentifyForLookup(sed: ActorRef)
  private case class IdentifyForLookupOrCreate(sed: ActorRef, propsMaker: () => Props, escapedName: String)
}

class LocalActorRegistry extends Actor {
  import ActorRegistry._
  import LocalActorRegistry._

  def receive = {
    case Lookup(name) =>
      val sel = context.actorSelection(escape(name))
      val sed = sender
      sel ! Identify(IdentifyForLookup(sed))

    case LookupOrCreate(name, propsMaker) =>
      val esc = escape(name)
      val sel = context.actorSelection(esc)
      val sed = sender
      sel ! Identify(IdentifyForLookupOrCreate(sed, propsMaker, esc))

    case ActorIdentity(IdentifyForLookup(sed), opt) =>
      sed ! opt

    case ActorIdentity(IdentifyForLookupOrCreate(sed, _, _), Some(actorRef)) =>
      sed ! (false, actorRef)

    case ActorIdentity(IdentifyForLookupOrCreate(sed, propsMaker, escapedName), None) =>
      val actorRef = context.actorOf(propsMaker(), escapedName)
      sed ! (true, actorRef)
  }
}

class RemoteActorRegistry extends Actor with Logger {
  import ActorRegistry._
  import RemoteActorRegistry._

  private var addrs:     IMap[String, String] = _
  private var localAddr: String               = _

  override def preStart() {
    addrs = Config.hazelcastInstance.getMap("xitrum/RemoteActorRegistry")

    val h    = Config.hazelcastInstance
    val lock = h.getLock(ACTOR_NAME)
    lock.lock()
    try {
      val cluster = h.getCluster

      // Register local node
      val id = cluster.getLocalMember.getUuid

      val nc    = Config.application.getConfig("akka.remote.netty.tcp")
      val host  = nc.getString("hostname")
      val port  = nc.getInt("port")
      localAddr = host + ":" + port

      addrs.put(id, localAddr)

      // Unregister remote node
      cluster.addMembershipListener(new MembershipListener {
        def memberAdded(membershipEvent: MembershipEvent) {}

        def memberRemoved(membershipEvent: MembershipEvent) {
          val id = membershipEvent.getMember.getUuid
          addrs.remove(id)
        }
      })
    } finally {
      lock.unlock()
    }
  }

  def receive = {
    case Lookup(name) =>
      sender ! lookup(name)

    case LookupLocal(name: String) =>
      sender ! lookupLocal(name)

    case LookupOrCreate(name, propsMaker) =>
      sender ! lookupOrCreate(name, propsMaker)
  }

  //----------------------------------------------------------------------------

  private def lookupLocal(name: String): Option[ActorRef] = {
    val sel    = context.actorSelection(escape(name))
    val future = ask(sel, Identify(None))(LOOKUP_TIMEOUT).mapTo[ActorIdentity].map(_.ref)

    try {
      Await.result(future, LOOKUP_TIMEOUT)
    } catch {
      case NonFatal(e) =>
        logger.warn("lookupLocal Await error, name: " + name, e)
        None
    }
  }
  /*

  private def lookupLocal(name: String): Option[ActorRef] = {
    val sel    = context.actorSelection(escape(name))
    sel ! Identify(None)
    context.become(receivex)
    None
  }

  def receivex: PartialFunction[Any, Unit] = {
    case ActorIdentity(_, Some(ref)) ⇒
      println("---------- " + ref)
    case ActorIdentity(_, None) =>

      println("---------- " + None)

  }
*/
  private def lookupRemote(addr: String, name: String): Option[ActorRef] = {
    val url    = "akka.tcp://" + Config.ACTOR_SYSTEM_NAME + "@" + addr + "/user/" + ACTOR_NAME
    val sel    = context.actorSelection(url)
    val future = sel.ask(LookupLocal(name))(LOOKUP_TIMEOUT).mapTo[Option[ActorRef]]
    try {
      Await.result(future, LOOKUP_TIMEOUT)
    } catch {
      case NonFatal(e) =>
        logger.warn("lookupRemote Await error, addr: " + addr + ", name: " + name, e)
        None
    }
  }

  private def lookup(name: String): Option[ActorRef] = {
    val lock = Config.hazelcastInstance.getLock(ACTOR_NAME + name)
    lock.lock()
    try {
      val it = addrs.values().iterator
      while (it.hasNext()) {
        val addr = it.next()
        val refo = if (addr == localAddr) lookupLocal(name) else lookupRemote(addr, name)
        if (refo.isDefined) return refo
      }

      None
    } finally {
      lock.unlock()
    }
  }

  /** If the actor has not been created, it will be created locally. */
  private def lookupOrCreate(name: String, propsMaker: () => Props): (Boolean, ActorRef) = {
    val lock = Config.hazelcastInstance.getLock(ACTOR_NAME + name)
    lock.lock()
    try {
      val it = addrs.values().iterator
      while (it.hasNext()) {
        val addr = it.next()
        val refo = if (addr == localAddr) lookupLocal(name) else lookupRemote(addr, name)
        if (refo.isDefined) return (false, refo.get)
      }

      // Create local actor

      println("-----------Create local actor")
      (true, context.actorOf(propsMaker(), escape(name)))
    } finally {
      lock.unlock()
    }
  }
}
