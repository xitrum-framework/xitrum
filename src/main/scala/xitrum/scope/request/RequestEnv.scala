package xitrum.scope.request

import scala.collection.mutable.{Map => MMap}

import xitrum.{Config, Action}
import xitrum.handler.HandlerEnv
import xitrum.routing.Route

object RequestEnv {
  def inspectParamsWithFilter(params: MMap[String, _ <: Seq[AnyRef]]): String = {
    val sb = new StringBuilder
    sb.append("{")

    val keys = params.keys.toArray
    val size = keys.size
    for (i <- 0 until size) {
      val key = keys(i)

      sb.append(key)
      sb.append(": ")

      if (Config.xitrum.request.filteredParams.contains(key)) {
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
trait RequestEnv extends ParamAccess {
  this: Action =>

  // Below are lazy because they are not always accessed by framwork/application
  // (to save calculation time) or the things they depend on are null when this
  // instance is created

  lazy val at = new At

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
   * A merge of all text params (uriParams, bodyParams, pathParams), as contrast
   * to file upload (fileParams).
   *
   * A val not a def, for speed, so that the calculation is done only once.
   *
   * lazy, so that bodyParams can be changed by ValidatorCaller.
   * Because this is a val, once this is accessed, either of the 3 params should
   * not be changed, because the change will not be reflected. If you still want
   * to change the the 3 params, after changing them, please also change this
   * textParams.
   */
  lazy val textParams: Params = {
    val ret = MMap[String, Seq[String]]()

    // The order is important because we want the later to overwrite the former
    ret ++= handlerEnv.uriParams
    ret ++= handlerEnv.bodyParams
    ret ++= handlerEnv.pathParams

    ret
  }
}
