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

  /** The listener returns true if it wants itself to be removed. */
  type MessageListener = (CometMessage) => Boolean

  private[this] val messageListeners = MMap[String, ArrayBuffer[MessageListener]]()

  //----------------------------------------------------------------------------

  map.addIndex("topic",     false)    // Comparison: =
  map.addIndex("timestamp", true)   // Comparison: >

  map.addEntryListener(new EntryListener[Long, CometMessage] {
    def entryAdded(event: EntryEvent[Long, CometMessage]) {
      messageListeners.synchronized {
        val cm = event.getValue
        messageListeners.get(cm.topic).foreach { arrayBuffer =>
          val tobeRemoveds = ArrayBuffer[MessageListener]()

          arrayBuffer.foreach { listener =>
            if (listener.apply(cm)) tobeRemoveds.append(listener)
          }

          arrayBuffer --= tobeRemoveds
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

  def getMessages(channel: String, lastTimestamp: Long): Iterable[CometMessage] = {
    val pb = new PredicateBuilder
    val eo = pb.getEntryObject
    val p  = eo.get("topic").equal(channel).and(eo.get("timestamp").greaterThan(long2Long(lastTimestamp)))

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

  def unsubscribe(channel: String, listener: MessageListener) {
    messageListeners.synchronized {
      messageListeners.get(channel).foreach { arrayBuffer =>
        arrayBuffer -= listener

        // Avoid memory leak when there are too many empty entries
        if (arrayBuffer.isEmpty) messageListeners -= channel
      }
    }
  }
}
