package xitrum.scope.request

import scala.collection.mutable.{Map => MMap}
import io.netty.handler.codec.http.multipart.FileUpload

import xitrum.{Config, Action}
import xitrum.handler.HandlerEnv
import xitrum.routing.Route
import xitrum.util.SeriDeseri

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

      // Log is ouput after the response has been sent.
      // For FileUpload, the file will be cleaned up so FileUpload#toString
      // will throw null pointer exception.
      if (Config.xitrum.request.filteredParams.contains(key)) {
        sb.append("[FILTERED]")
      } else {
        val values = params(key)
        if (values.length == 0) {
          sb.append("[EMPTY]")
        } else if (values.length == 1) {
          val value = values(0)
          if (value.isInstanceOf[FileUpload])
            sb.append("<file>")
          else
            sb.append(values(0))
        } else {
          sb.append("[")
          val value = values(0)
          if (value.isInstanceOf[FileUpload])
            sb.append("<files>")
          else
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

  var handlerEnv: HandlerEnv = _

  def apply(handlerEnv: HandlerEnv) {
    this.handlerEnv = handlerEnv

    // Javadoc of Netty says channel.remoteAddress may be
    // "null if this channel is not connected".
    //
    // remoteAddress is used to calculate remoteIp. Force remoteIp here while
    // remoteAddress is not null.
    remoteIp
  }

  // Below are lazy because they are not always accessed by framwork/application
  // (to save calculation time) or the things they depend on are null when this
  // instance is created

  // Shortcuts to handlerEnv for easy access for app developers
  lazy val channel        = handlerEnv.channel
  lazy val request        = handlerEnv.request
  lazy val response       = handlerEnv.response
  lazy val queryParams    = handlerEnv.queryParams
  lazy val bodyTextParams = handlerEnv.bodyTextParams
  lazy val pathParams     = handlerEnv.pathParams
  lazy val textParams     = handlerEnv.textParams
  lazy val urlParams      = handlerEnv.urlParams
  lazy val bodyFileParams = handlerEnv.bodyFileParams

  lazy val at = new At

  def atJson(key: String) = at.toJson(key)

  lazy val requestContentString = {
    val byteBuf = request.content
    byteBuf.toString(Config.xitrum.request.charset)
  }

  /** Ex: val json = requestContentJson[Map[String, String]] */
  def requestContentJson[T](implicit m: Manifest[T]): Option[T] =
    SeriDeseri.fromJson[T](requestContentString)(m)
}
