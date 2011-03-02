package xitrum.action.env

import scala.collection.mutable.{Map => MMap, HashMap => MHashMap}

import org.jboss.netty.channel.ChannelHandlerContext
import org.jboss.netty.handler.codec.http.{FileUpload, HttpRequest}

import xitrum.Config
import xitrum.handler.{Env => HEnv}

object Env {
  type Params           = MMap[String, Array[String]]
  type FileUploadParams = MMap[String, Array[FileUpload]]

  /** Array[String].toString becomes somthing like [Ljava.lang.String;@29fb6448, which is meaningless */
  def inspectParams(params: MMap[String, Array[Any]]): String = {
    val sb = new StringBuilder
    sb.append("{")

    val keys = params.keys.toArray
    val size = keys.size
    for (i <- 0 until size) {
      val key    = keys(i)

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
class Env {
  import Env._

  var ctx:        ChannelHandlerContext = _
  var henv:       HEnv                  = _

  // Shortcuts from henv for easy access for web developers

  var request:    HttpRequest = _
  var pathInfo:   PathInfo    = _

  var uriParams:        Params = _
  var bodyParams:       Params = _
  var fileUploadParams: FileUploadParams = _
  var pathParams:       Params = _

  /**
   * text (uriParams, bodyParams, pathParams)  vs file upload (fileParams)
   *
   * Lazily initialized, not initialized in "apply" so that bodyParams can be
   * changed by ValidatorCaller. Because this is a lazy val, once this is accessed,
   * the 3 params should not be changed, because the change will not be reflected
   * by this val.
   *
   * Not a function ("def") so that the calculation is done only once.
   */
  lazy val textParams: Params = {
    val ret = new MHashMap[String, Array[String]]
    // The order is important because we want the later to overwrite the former
    ret ++= uriParams
    ret ++= bodyParams
    ret ++= pathParams
    ret
  }

  def apply(ctx: ChannelHandlerContext, henv: HEnv) {
    this.ctx        = ctx
    this.henv       = henv

    request    = henv.request
    pathInfo   = henv.pathInfo

    uriParams        = henv.uriParams
    bodyParams       = henv.bodyParams
    fileUploadParams = henv.fileUploadParams
    pathParams       = henv.pathParams
  }
}
