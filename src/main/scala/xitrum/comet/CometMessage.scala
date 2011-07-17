package xitrum.comet

import java.io.Serializable
import xitrum.scope.request.Params

class CometMessage(val channel: String, val timestamp: Long, val body: Params) extends Serializable
