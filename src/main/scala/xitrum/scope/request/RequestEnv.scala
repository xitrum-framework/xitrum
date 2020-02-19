package xitrum.scope.request

import io.netty.channel.Channel
import io.netty.handler.codec.http.{FullHttpRequest, FullHttpResponse}

import scala.collection.mutable.{Map => MMap}
import io.netty.handler.codec.http.multipart.FileUpload
import org.json4s.JValue
import xitrum.{Action, Config}
import xitrum.handler.HandlerEnv
import xitrum.util.SeriDeseri

object RequestEnv {
  private val LOG_LIMIT = 16

  def inspectParamsWithFilter(params: MMap[String, _ <: Seq[AnyRef]]): String = {
    val sb = new StringBuilder
    sb.append("{")

    val keys = params.keys.toArray
    val size = keys.length
    for (i <- 0 until size) {
      val key = keys(i)

      sb.append(limitLog(key))
      sb.append(": ")

      // Log is ouput after the response has been sent.
      // For FileUpload, the file will be cleaned up so FileUpload#toString
      // will throw null pointer exception.
      if (Config.xitrum.request.filteredParams.contains(key)) {
        sb.append("<filtered>")
      } else {
        val values = params(key)
        if (values.isEmpty) {
          sb.append("<empty>")
        } else if (values.length == 1) {
          val value = values.head
          if (value.isInstanceOf[FileUpload])
            sb.append("<file>")
          else
            sb.append(limitLog(values.head.toString))
        } else {
          sb.append("[")
          val value = values.head
          if (value.isInstanceOf[FileUpload])
            sb.append(s"<${values.size} files>")
          else
            sb.append(values.map(v => limitLog(v.toString)).mkString(", "))
          sb.append("]")
        }
      }

      if (i < size - 1) sb.append(", ")
    }

    sb.append("}")
    sb.toString
  }

  private def limitLog(string: String): String = {
    if (string.length <= LOG_LIMIT + 3) {  // 3: length of "..."
      string
    } else {
      string.substring(0, LOG_LIMIT) + "..."
    }
  }
}

/**
 * All core state variables for a request are here. All other variables in Helper
 * and Controller can be inferred from these variables.
 */
trait RequestEnv extends ParamAccess {
  this: Action =>

  var handlerEnv: HandlerEnv = _

  def apply(handlerEnv: HandlerEnv): Unit = {
    this.handlerEnv = handlerEnv

    // Javadoc of Netty says channel.remoteAddress may be
    // "null if this channel is not connected".
    //
    // remoteAddress is used to calculate remoteIp. Force remoteIp here while
    // remoteAddress is not null.
    remoteIp
  }

  // The below are lazy because they are not always accessed by framwork/application
  // (to save calculation time) or the things they depend on are null when this
  // instance is created

  // Shortcuts to handlerEnv for easy access for app developers;
  // comments are copied from HandlerEnv.scala

  lazy val channel: Channel = handlerEnv.channel
  lazy val request: FullHttpRequest = handlerEnv.request
  lazy val response: FullHttpResponse = handlerEnv.response

  /** Params in request body. */
  lazy val bodyTextParams: Params = handlerEnv.bodyTextParams

  /** File params in request body. */
  lazy val bodyFileParams: FileUploadParams = handlerEnv.bodyFileParams

  /** Params embedded in the path. Ex: /articles/:id */
  lazy val pathParams: Params = handlerEnv.pathParams

  /** Params after the question mark of the URL. Ex: /search?q=xitrum */
  lazy val queryParams: Params = handlerEnv.queryParams

  /** The merge of queryParams and pathParams, things that appear in the request URL. */
  lazy val urlParams: Params = handlerEnv.urlParams

  /**
   * The merge of all text params (queryParams, bodyParams, and pathParams),
   * as contrast to file upload (bodyFileParams).
   */
  lazy val textParams: Params = handlerEnv.textParams

  /** The whole request body as String. */
  lazy val requestContentString: String = handlerEnv.requestContentString

  /**
   * The whole request body parsed as JSON4S JValue. You can use [[SeriDeseri.fromJValue]]
   * to convert this to Scala object (case class, Map, Seq etc.).
   */
  lazy val requestContentJValue: JValue = handlerEnv.requestContentJValue

  lazy val at = new At
  def atJson(key: String): String = at.toJson(key)
}
