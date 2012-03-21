package xitrum.comet

import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import com.hazelcast.core.{EntryEvent, EntryListener, IMap}
import com.hazelcast.query.PredicateBuilder

import xitrum.Config
import xitrum.scope.request.Params

// TODO: presense
object Comet {
  private[this] val TTL_SECONDS = 60

  private[this] val map = Config.hazelcastInstance.getMap("xitrum/comet").asInstanceOf[IMap[Long, CometMessage]]

  /** The listener returns true if it wants itself to be unsubscribe. */
  type MessageListener = (Seq[CometMessage]) => Boolean

  private[this] val messageListeners = MMap[String, ArrayBuffer[MessageListener]]()

  //----------------------------------------------------------------------------

  map.addIndex("topic",     false)  // Comparison: =
  map.addIndex("timestamp", true)   // Comparison: >

  map.addEntryListener(new EntryListener[Long, CometMessage] {
    def entryAdded(event: EntryEvent[Long, CometMessage]) {
      messageListeners.synchronized {
        val cm = event.getValue
        messageListeners.get(cm.topic).foreach { arrayBuffer =>
          val tobeUnsubscribes = ArrayBuffer[MessageListener]()

          arrayBuffer.foreach { listener =>
            if (listener.apply(Seq(cm))) tobeUnsubscribes.append(listener)
          }

          arrayBuffer --= tobeUnsubscribes
        }
      }
    }

    def entryEvicted(event: EntryEvent[Long, CometMessage]) {}
    def entryRemoved(event: EntryEvent[Long, CometMessage]) {}
    def entryUpdated(event: EntryEvent[Long, CometMessage]) {}
  }, true)

  //----------------------------------------------------------------------------

  def publish(topic: String, message: Params) {
    val timestamp = System.currentTimeMillis()
    val cm        = new CometMessage(topic, timestamp, message)
    map.put(timestamp, cm, TTL_SECONDS, TimeUnit.SECONDS)
  }

  def getMessages(topic: String, lastTimestamp: Long): Seq[CometMessage] = {
    val pb = new PredicateBuilder
    val eo = pb.getEntryObject
    val p  = eo.get("topic").equal(topic).and(eo.get("timestamp").greaterThan(long2Long(lastTimestamp)))

    val javaCollection = map.values(p)
    javaCollection.asScala.toList.sortBy(_.timestamp)
  }

  //----------------------------------------------------------------------------

  def subscribe(topic: String, listener: MessageListener) {
    messageListeners.synchronized {
      messageListeners.get(topic) match {
        case None              => messageListeners(topic) = ArrayBuffer(listener)
        case Some(arrayBuffer) => arrayBuffer.append(listener)
      }
    }
  }

  /**
   * Subscribe and get existing messages at the same time.
   * @return true if the listener has been subscribed
   */
  def subscribe(topic: String, listener: MessageListener, lastTimestamp: Long): Boolean = {
    messageListeners.synchronized {
      val messages = getMessages(topic, lastTimestamp)
      if (messages.isEmpty) {
        messageListeners.get(topic) match {
          case None              => messageListeners(topic) = ArrayBuffer(listener)
          case Some(arrayBuffer) => arrayBuffer.append(listener)
        }
        true
      } else {
        val shouldBeSubscribed = !listener.apply(messages)
        if (shouldBeSubscribed) {
          messageListeners.get(topic) match {
            case None              => messageListeners(topic) = ArrayBuffer(listener)
            case Some(arrayBuffer) => arrayBuffer.append(listener)
          }
        }
        shouldBeSubscribed
      }
    }
  }

  def unsubscribe(topic: String, listener: MessageListener) {
    messageListeners.synchronized {
      messageListeners.get(topic).foreach { arrayBuffer =>
        arrayBuffer -= listener

        // Avoid memory waste when there are too many empty entries
        if (arrayBuffer.isEmpty) messageListeners -= topic
      }
    }
  }
}
