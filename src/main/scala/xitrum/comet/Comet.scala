package xitrum.comet

import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import com.hazelcast.core.{EntryEvent, EntryListener, Hazelcast, IMap}
import com.hazelcast.query.PredicateBuilder

// TODO: presense
object Comet {
  private val MAP_NAME    = "xitrum/comet"
  private val TTL_SECONDS = 60

  private val map = Hazelcast.getMap(MAP_NAME).asInstanceOf[IMap[Long, CometMessage]]

  /** The listener returns true if it wants itself to be removed. */
  type MessageListener = (CometMessage) => Boolean

  private val messageListeners = MMap[String, ArrayBuffer[MessageListener]]()

  //----------------------------------------------------------------------------

  map.addIndex("channel",   false)  // Comparison: =
  map.addIndex("timestamp", true)   // Comparison: >
  
  map.addEntryListener(new EntryListener[Long, CometMessage] {
    def entryAdded(event: EntryEvent[Long, CometMessage]) {
      messageListeners.synchronized {
        val cm = event.getValue
        messageListeners.get(cm.channel) match {
          case None =>

          case Some(arrayBuffer) =>
            val tobeRemoved = ArrayBuffer[MessageListener]()

            arrayBuffer.foreach { listener =>
              if (listener.apply(cm)) tobeRemoved.append(listener)
            }

            arrayBuffer --= tobeRemoved
        }
      }
    }
    
    def entryEvicted(event: EntryEvent[Long, CometMessage]) {}
    def entryRemoved(event: EntryEvent[Long, CometMessage]) {}
    def entryUpdated(event: EntryEvent[Long, CometMessage]) {}
  }, true)

  //----------------------------------------------------------------------------

  def publish(channel: String, message: String) {
    val timestamp = System.currentTimeMillis
    val cm        = new CometMessage(channel, timestamp, message)
    map.put(timestamp, cm, TTL_SECONDS, TimeUnit.SECONDS)
  }

  def getMessages(channel: String, lastTimestamp: Long): Iterable[CometMessage] = {
    val pb = new PredicateBuilder
    val eo = pb.getEntryObject
    val p  = eo.get("channel").equal(channel).and(eo.get("timestamp").greaterThan(long2Long(lastTimestamp)))

    val javaCollection = map.values(p)
    javaCollection.asScala.toList.sortBy(_.timestamp)
  }

  //----------------------------------------------------------------------------

  def addMessageListener(channel: String, listener: MessageListener) {
    messageListeners.synchronized { 
      messageListeners.get(channel) match {
        case None              => messageListeners(channel) = ArrayBuffer(listener)
        case Some(arrayBuffer) => arrayBuffer.append(listener)
      }
    }
  }

  def removeMessageListener(channel: String, listener: MessageListener) {
    messageListeners.synchronized { 
      messageListeners.get(channel) match {
        case None =>

        case Some(arrayBuffer) =>
          arrayBuffer -= listener

          // Avoid memory leak when there are too many empty entries
          if (arrayBuffer.isEmpty) messageListeners -= channel  
      }
    }
  }
}
