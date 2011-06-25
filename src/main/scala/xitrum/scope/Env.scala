package xitrum.scope

import scala.collection.mutable.{Map => MMap}

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
  var ctx:  ChannelHandlerContext = _
  var henv: HEnv                  = _

  def apply(ctx: ChannelHandlerContext, henv: HEnv) {
    this.ctx  = ctx
    this.henv = henv
  }

  // Shortcuts to henv for easy access for app developers
  // "def" is used instead of "val" or "var" to to synchronize with henv
  def request          = henv.request
  def pathInfo         = henv.pathInfo
  def fileUploadParams = henv.fileUploadParams
}
