package xitrum.handler.up

import java.nio.charset.Charset
import scala.collection.mutable.{Map => MMap}

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpRequest}
import org.jboss.netty.handler.codec.http.multipart.{Attribute, DefaultHttpDataFactory, DiskAttribute, DiskFileUpload, FileUpload, HttpPostRequestDecoder, InterfaceHttpData}
import ChannelHandler.Sharable
import InterfaceHttpData.HttpDataType

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.scope.request.{FileUploadParams, Params, PathInfo}

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
  val factory = new DefaultHttpDataFactory(Config.config.request.maxSizeInMB * 1024 * 1024)
}

@Sharable
class BodyParser extends SimpleChannelUpstreamHandler with BadClientSilencer {
  import BodyParser._

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendUpstream(e)
      return
    }

    val handlerEnv = m.asInstanceOf[HandlerEnv]
    val request    = handlerEnv.request

    try {
      val (bodyParams, fileUploadParams) = decodeRequestBody(request)
      handlerEnv.bodyParams       = bodyParams
      handlerEnv.fileUploadParams = fileUploadParams
      Channels.fireMessageReceived(ctx, handlerEnv)
    } catch {
      case e: Exception =>
        val msg = "Could not parse body of request: " + request
        logger.warn(msg, e)
        ctx.getChannel.close()
    }
  }

  //----------------------------------------------------------------------------

  // May throw exception on decode error
  private def decodeRequestBody(request: HttpRequest): (MMap[String, List[String]], MMap[String, List[FileUpload]]) = {
    val method = request.getMethod
    if (!method.equals(HttpMethod.POST) && !method.equals(HttpMethod.PUT) && !method.equals(HttpMethod.PATCH))
      return (MMap[String, List[String]](), MMap[String, List[FileUpload]]())

    val requestContentType = request.getHeader(HttpHeaders.Names.CONTENT_TYPE)
    if (requestContentType == null)
      return (MMap[String, List[String]](), MMap[String, List[FileUpload]]())

    val requestContentTypeLowerCase = requestContentType.toLowerCase
    if (!requestContentTypeLowerCase.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED) &&
        !requestContentTypeLowerCase.startsWith(HttpHeaders.Values.MULTIPART_FORM_DATA))
      return (MMap[String, List[String]](), MMap[String, List[FileUpload]]())

    val decoder = new HttpPostRequestDecoder(factory, request)
    val datas   = decoder.getBodyHttpDatas

    val bodyParams = MMap[String, List[String]]()
    val fileParams = MMap[String, List[FileUpload]]()

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
  }

  private def sanitizeFileUploadFilename(fileUpload: FileUpload) {
    val filename1 = fileUpload.getFilename
    val filename2 = filename1.split('/').last.split('\\').last.trim.replaceAll("^\\.+", "")
    val filename3 = if (filename2.isEmpty) "filename" else filename2
    fileUpload.setFilename(filename3)
  }

  private def putOrAppendString(map: Params, key: String, value: String) {
    if (!map.contains(key)) {
      map(key) = List(value)
    } else {
      val values = map(key)
      map(key) = values :+ value
    }
  }

  private def putOrAppendFileUpload(map: FileUploadParams, key: String, value: FileUpload) {
    if (!map.contains(key)) {
      map(key) = List(value)
    } else {
      val values = map(key)
      map(key) = values :+ value
    }
  }
}
