package xitrum.util

import java.net.URLEncoder

import scala.collection.mutable.{Map => MMap}
import scala.concurrent.duration._
import scala.concurrent.Await

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask

import com.hazelcast.core.{IMap, MembershipEvent, MembershipListener}

import xitrum.Config

/**
 * This feature is implemented using Akka Remoting and Hazelcast distributed lock:
 * https://groups.google.com/group/akka-user/browse_thread/thread/23d6b2851648c1b0
 *
 * You must config config/akka.conf to use Akka Remoting:
 * http://doc.akka.io/docs/akka/2.1.2/scala/remoting.html
 *
 * With future version of Akka Cluster, this object may be removed:
 * http://doc.akka.io/docs/akka/2.1.2/cluster/index.html
 *
 * Caveat: Akka Remoting does not trigger Death Watch for lost connections.
 */
object ClusterSingletonActor {
  case class Lookup(name: String)
  case class LookupLocal(name: String)
  case class LookupOrCreate(name: String, propsMaker: () => Props)

  private val ACTOR_SYSTEM_NAME     = getClass.getName
  private val REMOTE_LOOKUP_TIMEOUT = 5.seconds

  def start() {
    Config.actorSystem.actorOf(Props[ClusterSingletonActor], ACTOR_SYSTEM_NAME)
  }

  def actor() = Config.actorSystem.actorFor("user/" + ACTOR_SYSTEM_NAME)
}

class ClusterSingletonActor extends Actor {
  import ClusterSingletonActor._

  private var addrs:     IMap[String, String] = _
  private var localAddr: String               = _

  override def preStart() {
    addrs = Config.hazelcastInstance.getMap("xitrum/ClusterSingletonActor")

    val h    = Config.hazelcastInstance
    val lock = h.getLock(ACTOR_SYSTEM_NAME)
    lock.lock()
    try {
      val cluster = h.getCluster

      // Register local node
      val id = cluster.getLocalMember.getUuid

      val nc    = Config.application.getConfig("akka.remote.netty")
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
    case Lookup(name: String) =>
      sender ! lookup(name)

    case LookupLocal(name: String) =>
      sender ! lookupLocal(name)

    case LookupOrCreate(name, propsMaker) =>
      sender ! lookupOrCreate(name, propsMaker)
  }

  //----------------------------------------------------------------------------

  private def lookup(name: String): Option[ActorRef] = {
    val escapedName = escape(name)
    val h           = Config.hazelcastInstance
    val lock        = h.getLock(ACTOR_SYSTEM_NAME)
    lock.lock()
    try {
      val it = addrs.values().iterator
      while (it.hasNext()) {
        val addr = it.next()
        if (addr == localAddr) {
          val ref = context.actorFor(escapedName)
          if (!ref.isTerminated) return Some(ref)
        } else {
          val remoteSingleActor = context.actorFor(
            "akka://" + Config.ACTOR_SYSTEM_NAME + "@" + addr + "/user/" + ACTOR_SYSTEM_NAME)
          val future            = remoteSingleActor.ask(LookupLocal(name))(REMOTE_LOOKUP_TIMEOUT)
          val refo              = Await.result(future, REMOTE_LOOKUP_TIMEOUT).asInstanceOf[Option[ActorRef]]
          if (refo.isDefined) return refo
        }
      }

      None
    } finally {
      lock.unlock()
    }
  }

  private def lookupLocal(name: String): Option[ActorRef] = {
    val escapedName = escape(name)
    val ref         = context.actorFor(escapedName)
    if (ref.isTerminated) None else Some(ref)
  }

  /** If the actor has not been created, it will be created locally. */
  private def lookupOrCreate(name: String, propsMaker: () => Props): (Boolean, ActorRef) = {
    val escapedName = escape(name)
    val h           = Config.hazelcastInstance
    val lock        = h.getLock(ACTOR_SYSTEM_NAME)
    lock.lock()
    try {
      val it = addrs.values().iterator
      while (it.hasNext()) {
        val addr = it.next()
        if (addr == localAddr) {
          val ref = context.actorFor(escapedName)
          if (!ref.isTerminated) return (false, ref)
        } else {
          val remoteSingleActor = context.actorFor(
            "akka://" + Config.ACTOR_SYSTEM_NAME + "@" + addr + "/user/" + ACTOR_SYSTEM_NAME)
          val future            = remoteSingleActor.ask(LookupLocal(name))(REMOTE_LOOKUP_TIMEOUT)
          val refo              = Await.result(future, REMOTE_LOOKUP_TIMEOUT).asInstanceOf[Option[ActorRef]]
          if (refo.isDefined) return (false, refo.get)
        }
      }

      // Create local actor
      (true, context.actorOf(propsMaker(), escapedName))
    } finally {
      lock.unlock()
    }
  }

  private def escape(name: String) = URLEncoder.encode(name, "UTF-8")
}
