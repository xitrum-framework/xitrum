package xitrum.comet

import java.io.Serializable

class CometMessage(val channel: String, val timestamp: Long, val body: String) extends Serializable
