package xitrum.comet

import xitrum.scope.request.Params

class CometMessage(val channel: String, val timestamp: Long, val body: Params)
