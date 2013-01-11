package xitrum.util

import scala.collection.mutable.{Map => MMap}

import akka.actor.{ActorRef, Props}
import com.hazelcast.core.{IMap, MembershipEvent, MembershipListener}

import xitrum.Config

/**
 * This object provides "single actor instance" feature, implemented using
 * Akka Remoting and Hazelcast:
 * https://groups.google.com/group/akka-user/browse_thread/thread/23d6b2851648c1b0
 *
 * You must config config/akka.conf to use Akka Remoting:
 * http://doc.akka.io/docs/akka/2.1.0/scala/remoting.html
 *
 * With future version of Akka, this object may become obsolete.
 */
object SingleActorInstance {
  private val LOCK_NAME = getClass.getName

  private val addrs: IMap[String, String] = Config.hazelcastInstance.getMap("xitrum/SingleActorInstance")

  {
    val h    = Config.hazelcastInstance
    val lock = h.getLock(LOCK_NAME)
    lock.lock()
    try {
      val cluster = h.getCluster

      // Register local node
      val id = cluster.getLocalMember.getUuid

      val nc   = Config.application.getConfig("akka.remote.netty")
      val host = nc.getString("hostname")
      val port = nc.getInt("port")
      val addr = host + ":" + port

      println("addrs.put(id, addr)", id, addr)
      addrs.put(id, addr)

      // Unregister remote node
      cluster.addMembershipListener(new MembershipListener {
        def memberAdded(membershipEvent: MembershipEvent) {}

        def memberRemoved(membershipEvent: MembershipEvent) {
          val id = membershipEvent.getMember.getUuid
          println("addrs.remove(id)", id)
          addrs.remove(id)
        }
      })
    } finally {
      lock.unlock()
    }
  }

  /** If the actor has not been created, it will be created locally. */
  def lookupOrCreate(name: String, propsMaker: () => Props): (Boolean, ActorRef) = {
    val a    = Config.actorSystem
    val h    = Config.hazelcastInstance
    val lock = h.getLock(LOCK_NAME)
    lock.lock()
    try {
      val it = addrs.values().iterator
      while (it.hasNext()) {
        val addr = it.next()
        println("addr", addr, "akka://" + Config.ACTOR_SYSTEM_NAME + "@" + addr + "/user/" + name)
        val ref  = a.actorFor("akka://" + Config.ACTOR_SYSTEM_NAME + "@" + addr + "/user/" + name)
        if (!ref.isTerminated) return (false, ref)
      }

      // Create local actor
      (true, a.actorOf(propsMaker(), name))
    } finally {
      lock.unlock()
    }
  }

  def lookup(name: String): Option[ActorRef] = {
    val a    = Config.actorSystem
    val h    = Config.hazelcastInstance
    val lock = h.getLock(LOCK_NAME)
    lock.lock()
    try {
      val it = addrs.values().iterator
      while (it.hasNext()) {
        val addr = it.next()
        val ref  = a.actorFor("akka://" + Config.ACTOR_SYSTEM_NAME + "@" + addr + "/user/" + name)
        if (!ref.isTerminated) return Some(ref)
      }

      None
    } finally {
      lock.unlock()
    }
  }
}
