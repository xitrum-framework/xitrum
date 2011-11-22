package xitrum.scope.request

import scala.collection.mutable.{Map => MMap}

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

      if (Config.config.request.filteredParams.contains(key)) {
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
  var handlerEnv: HandlerEnv = _

  def apply(handlerEnv: HandlerEnv) {
    this.handlerEnv = handlerEnv
  }

  // Shortcuts to handlerEnv for easy access for app developers
  def channel          = handlerEnv.channel
  def request          = handlerEnv.request
  def response         = handlerEnv.response
  def pathInfo         = handlerEnv.pathInfo
  def uriParams        = handlerEnv.uriParams
  def bodyParams       = handlerEnv.bodyParams
  def pathParams       = handlerEnv.pathParams
  def fileUploadParams = handlerEnv.fileUploadParams

  /**
   * text (uriParams, bodyParams, pathParams) vs file upload (fileParams)
   *
   * Lazily initialized, so that bodyParams can be
   * changed by ValidatorCaller. Because this is a lazy val, once this is accessed,
   * the 3 params should not be changed, because the change will not be reflected
   * by this val.
   *
   * Not a function ("def") so that the calculation is done only once.
   */
  lazy val textParams: Params = {
    val ret = MMap[String, List[String]]()

    // The order is important because we want the later to overwrite the former
    ret ++= handlerEnv.uriParams
    ret ++= handlerEnv.bodyParams
    ret ++= handlerEnv.pathParams

    ret
  }
}
