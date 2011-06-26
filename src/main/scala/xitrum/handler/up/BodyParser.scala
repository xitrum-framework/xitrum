package xitrum.handler.up

import java.nio.charset.Charset
import scala.collection.mutable.{HashMap => MHashMap}

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{Attribute, DefaultHttpDataFactory, DiskAttribute, DiskFileUpload, FileUpload, HttpRequest, HttpMethod, HttpPostRequestDecoder, InterfaceHttpData}
import HttpMethod._
import InterfaceHttpData.HttpDataType

import xitrum.Config
import xitrum.handler.Env
import xitrum.scope.{Env => AEnv, PathInfo}

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
  import AEnv._
  import BodyParser._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[Env]
    val request = env.request

    val (bodyParams, fileUploadParams) = if (request.getMethod != POST) {
      (new MHashMap[String, Array[String]], new MHashMap[String, Array[FileUpload]])
    } else {
      try {
        val bodyParams = new MHashMap[String, Array[String]]
        val fileParams = new MHashMap[String, Array[FileUpload]]

        val decoder = new HttpPostRequestDecoder(factory, request)
        val datas   = decoder.getBodyHttpDatas

        val it = datas.iterator
        while (it.hasNext) {
          val data = it.next
          if (data.getHttpDataType == HttpDataType.Attribute) {
            val attribute = data.asInstanceOf[Attribute]
            val name      = attribute.getName
            val value     = attribute.getValue
            putOrAppendString(bodyParams, name, value)
          } else if (data.getHttpDataType == HttpDataType.FileUpload) {
            val fileUpload = data.asInstanceOf[FileUpload]
            if (fileUpload.isCompleted && fileUpload.length > 0) {  // Skip empty file
              val name = fileUpload.getName
              sanitizeFileUploadFilename(fileUpload)
              putOrAppendFileUpload(fileParams, name, fileUpload)
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

    env.bodyParams       = bodyParams
    env.fileUploadParams = fileUploadParams
    Channels.fireMessageReceived(ctx, env)
  }

  //----------------------------------------------------------------------------

  private def sanitizeFileUploadFilename(fileUpload: FileUpload) {
    val filename1 = fileUpload.getFilename
    val filename2 = filename1.split('/').last.split('\\').last.trim.replaceAll("^\\.+", "")
    val filename3 = if (filename2.isEmpty) "filename" else filename2
    fileUpload.setFilename(filename3)
  }

  private def putOrAppendString(map: MHashMap[String, Array[String]], key: String, value: String) {
    if (!map.contains(key)) {
      map(key) = Array(value)
    } else {
      val values = map(key)
      map(key) = values :+ value
    }
  }

  private def putOrAppendFileUpload(map: MHashMap[String, Array[FileUpload]], key: String, value: FileUpload) {
    if (!map.contains(key)) {
      map(key) = Array(value)
    } else {
      val values = map(key)
      map(key) = values :+ value
    }
  }
}
