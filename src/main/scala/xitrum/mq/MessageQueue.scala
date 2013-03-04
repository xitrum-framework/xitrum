package xitrum.mq

import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import com.hazelcast.core.{EntryEvent, EntryListener, IMap}
import com.hazelcast.query.PredicateBuilder

import xitrum.Config
import xitrum.scope.request.Params

/**
 * Very simple message queue. Messages older than 60 seconds will automatically
 * be removed.
 *
 * For more advanced features (presense, save to disk etc.), you should implement
 * your own message queue, based on Hazelcast.
 */
object MessageQueue {
  private[this] val TTL_SECONDS = 60

  private[this] val map = Config.hazelcastInstance.getMap("xitrum/message-queue").asInstanceOf[IMap[Long, QueueMessage]]

  /** The listener returns true if it wants itself to be unsubscribe. */
  type MessageListener = (Seq[QueueMessage]) => Boolean

  private[this] val messageListeners = MMap[String, ArrayBuffer[MessageListener]]()

  //----------------------------------------------------------------------------

  // Commented out for now, because of this issue:
  // https://github.com/hazelcast/hazelcast/issues/310
  //map.addIndex("topic",     false)  // Comparison: =
  //map.addIndex("timestamp", true)   // Comparison: >

  map.addEntryListener(new EntryListener[Long, QueueMessage] {
    def entryAdded(event: EntryEvent[Long, QueueMessage]) {
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

    def entryEvicted(event: EntryEvent[Long, QueueMessage]) {}
    def entryRemoved(event: EntryEvent[Long, QueueMessage]) {}
    def entryUpdated(event: EntryEvent[Long, QueueMessage]) {}
  }, true)

  //----------------------------------------------------------------------------

  def publish(topic: String, message: Any) {
    val timestamp = System.currentTimeMillis()
    val cm        = new QueueMessage(topic, timestamp, message)
    map.put(timestamp, cm, TTL_SECONDS, TimeUnit.SECONDS)
  }

  def getMessages(topic: String, lastTimestamp: Long): Seq[QueueMessage] = {
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
