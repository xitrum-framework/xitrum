package xitrum.handler.up

import java.util.{Collections, LinkedHashMap => JLinkedHashMap, List => JList, Map => JMap}
import java.nio.charset.Charset

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{Attribute, DefaultHttpDataFactory, DiskAttribute, DiskFileUpload, FileUpload, HttpRequest, HttpMethod, HttpPostRequestDecoder, InterfaceHttpData}
import HttpMethod._
import InterfaceHttpData.HttpDataType

import xitrum.Config
import xitrum.handler.Env
import xitrum.action.env.{Env => CEnv, PathInfo}
import xitrum.action.routing.Util

object BodyParser {
  DiskAttribute.deleteOnExitTemporaryFile  = true  // Should delete file on exit (in normal exit)
  DiskAttribute.baseDirectory              = null  // System temp directory

  DiskFileUpload.deleteOnExitTemporaryFile = true  // Should delete file on exit (in normal exit)
  DiskFileUpload.baseDirectory             = null  // System temp directory

  // Creating factory should be after the above for the factory to take effect of the settings

  // TODO: Use chunk mode, remove HttpChunkAggregator, see org.jboss.netty.example.http.upload.HttpRequestHandler
  //val factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE)  // Save to disk if size exceeds MINSIZE

  // "Save to disk if size exceeds MINSIZE" only works in chunk mode, not compatible with HttpChunkAggregator
  // When the file is too big, it will cause java.lang.NullPointerException: buffer (AbstractDiskHttpData.java:173)
  val factory = new DefaultHttpDataFactory(Config.maxRequestContentLengthInMB * 1024 * 1024)
}

@Sharable
class BodyParser extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
  import CEnv._
  import BodyParser._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[Env]
    val request = env.request

    val (bodyParams, fileParams): (Params, FileParams) = if (request.getMethod != POST) {
      (Collections.emptyMap[String, JList[String]], Collections.emptyMap[String, JList[FileUpload]])
    } else {
      try {
        val bodyParams = new JLinkedHashMap[String, JList[String]]
        val fileParams = new JLinkedHashMap[String, JList[FileUpload]]

        val decoder = new HttpPostRequestDecoder(factory, request)
        val datas   = decoder.getBodyHttpDatas

        val it = datas.iterator
        while (it.hasNext) {
          val data = it.next
          if (data.getHttpDataType == HttpDataType.Attribute) {
            val attribute = data.asInstanceOf[Attribute]
            val name      = attribute.getName
            val value     = attribute.getValue
            putOrAppendToList(bodyParams, name, value)
          } else if (data.getHttpDataType == HttpDataType.FileUpload) {
            val fileUpload = data.asInstanceOf[FileUpload]
            if (fileUpload.isCompleted && fileUpload.length > 0) {  // Skip empty file
              val name = fileUpload.getName
              putOrAppendToList(fileParams, name, fileUpload)
            }
          }
        }

        (bodyParams, fileParams)
      } catch {
        case t =>
          val msg = "Could not parse POST body, URI: " + request.getUri
          logger.warn(msg, t)

          ctx.getChannel.close
          return
      }
    }

    env.bodyParams = bodyParams
    env.fileParams = fileParams
    Channels.fireMessageReceived(ctx, env)
  }

  //----------------------------------------------------------------------------

  private def putOrAppendToList[T](map: JMap[String, JList[T]], key: String, value: T) {
    if (!map.containsKey(key)) {
      map.put(key, Util.toValues(value))
    } else {
      val values = map.get(key)
      values.add(value)
    }
  }
}
