package xitrum.sockjs

import xitrum.scope.request.Params

/** Needs to be Serializable to be stored in Hazelcast */
class QueueMessage(val topic: String, val timestamp: Long, val body: Any) extends Serializable {
  override def toString = {
    "@" + timestamp + " [" + topic + "] " + body
  }
}
