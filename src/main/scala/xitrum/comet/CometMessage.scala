package xitrum.comet

import xitrum.scope.request.Params

/** Needs to be Serializable to be stored in Hazelcast */
class CometMessage(val topic: String, val timestamp: Long, val body: Params) extends Serializable {
  override def toString = {
    "@" + timestamp + " [" + topic + "] " + body
  }
}
