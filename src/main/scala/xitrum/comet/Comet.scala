import java.util.concurrent.TimeUnit

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

import com.hazelcast.core.{EntryEvent, EntryListener, Hazelcast, IMap}
import com.hazelcast.query.PredicateBuilder

// TODO: presense
object Comet {
  private val MAP_NAME    = "xitrum/comet"
  private val TTL_SECONDS = 60

  private val map = Hazelcast.getMap(MAP_NAME).asInstanceOf[IMap[Long, CometMessage]]

  /** The listener returns true if it wants itself to be removed. */
  type MessageListener = (CometMessage) => Boolean

  private val messageListeners = new ArrayBuffer[MessageListener]()

  //----------------------------------------------------------------------------

  map.addIndex("channel",   false)  // Comparison: =
  map.addIndex("timestamp", true)   // Comparison: >
  
  map.addEntryListener(new EntryListener[Long, CometMessage] {
    def entryAdded(event: EntryEvent[Long, CometMessage]) {
      messageListeners.synchronized {
        val tobeRemoved = new ArrayBuffer[MessageListener]()

        messageListeners.foreach { listener =>
          if (listener.apply(event.getValue)) tobeRemoved.append(listener)
        }

        messageListeners --= tobeRemoved
      }
    }
    
    def entryEvicted(event: EntryEvent[Long, CometMessage]) {
    }
    
    def entryRemoved(event: EntryEvent[Long, CometMessage]) {
    }
    
    def entryUpdated(event: EntryEvent[Long, CometMessage]) {
    }
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
    val p  = eo.get("channel").equal(channel).and(eo.get("timestamp").greaterThan(lastTimestamp))

    val javaCollection = map.values(p)
    javaCollection.asScala.toList.sortBy(_.timestamp)
  }

  def addMessageListener(channel: String, listener: MessageListener) {
    messageListeners.synchronized { messageListeners.append(listener) }
  }
}
