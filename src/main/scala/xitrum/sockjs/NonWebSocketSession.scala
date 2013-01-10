package xitrum.sockjs

import com.hazelcast.core.IMap
import xitrum.Config

/** Manager for non-WebSocket SockJS sessions. */
object NonWebSocketSession {
  private val INFO_KEY = 0
  private val INFO_NO_SUBSCRIBER  = "0"
  private val INFO_ONE_SUBSCRIBER = "1"
  private val INFO_CLOSED         = "2"

  /** If the session does not exist, creates one and subscribes. */
  def subscribe(sessionId: String): NonWebSocketSession = {
    val map: IMap[Long, String] = Config.hazelcastInstance.getMap("xitrum/sockjs/" + sessionId)
    if (map.containsKey(INFO_KEY)) {

    } else {
      map.put(INFO_KEY, INFO_ONE_SUBSCRIBER)
    }


    null
  }
}

/**
 * Each instance of this class is a message queue for each non-WebSocket SockJS
 * session, with at most 1 subscriber. If there's no subscriber for 25 seconds,
 * this queue is automatically destroyed.
 */
class NonWebSocketSession(sessionId: String) {

}
