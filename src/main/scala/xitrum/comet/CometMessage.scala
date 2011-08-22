package xitrum.comet

import xitrum.scope.request.Params

// Needs to be Serializable to be stored in Hazelcast
class CometMessage(val channel: String, val timestamp: Long, val body: Params) extends Serializable
