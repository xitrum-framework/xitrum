package xitrum.scope.request

import scala.collection.mutable.{Map => MMap}
import org.jboss.netty.channel.ChannelHandlerContext

import xitrum.Config
import xitrum.handler.HandlerEnv

object RequestEnv {
  def inspectParamsWithFilter(params: MMap[String, List[Any]]): String = {
    val sb = new StringBuilder
    sb.append("{")

    val keys = params.keys.toArray
    val size = keys.size
    for (i <- 0 until size) {
      val key = keys(i)

      sb.append(key)
      sb.append(": ")

      if (Config.filteredParams.contains(key)) {
        sb.append("[FILTERED]")
      } else {
        val values = params(key)

        if (values.length == 0) {
          sb.append("[EMPTY]")
        } else if (values.length == 1) {
          sb.append(values(0))
        } else {
          sb.append("[")
          sb.append(values.mkString(", "))
          sb.append("]")
        }
      }

      if (i < size - 1) sb.append(", ")
    }

    sb.append("}")
    sb.toString
  }
}

/**
 * All core state variables for a request are here. All other variables in Helper
 * and Controller can be inferred from these variables.
 */
class RequestEnv {
  var ctx:        ChannelHandlerContext = _
  var handlerEnv: HandlerEnv            = _

  def apply(ctx: ChannelHandlerContext, handlerEnv: HandlerEnv) {
    this.ctx        = ctx
    this.handlerEnv = handlerEnv
  }

  // Shortcuts to handlerEnv for easy access for app developers
  def request          = handlerEnv.request
  def pathInfo         = handlerEnv.pathInfo
  def uriParams        = handlerEnv.uriParams
  def bodyParams       = handlerEnv.bodyParams
  def pathParams       = handlerEnv.pathParams
  def fileUploadParams = handlerEnv.fileUploadParams
}
