package xitrum.handler.outbound

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelHandler, ChannelHandlerContext, ChannelOutboundHandlerAdapter, ChannelPromise}
import io.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpRequest, FullHttpResponse, HttpResponseStatus}
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpMethod._
import HttpResponseStatus._

import xitrum.etag.Etag
import xitrum.handler.{AccessLog, HandlerEnv, NoRealPipelining}
import xitrum.util.{ByteBufUtil, Gzip}

object XSendResource {
  // setClientCacheAggressively should be called at PublicResourceServer, not
  // here because XSendResource may be used by applications which does not want
  // to clients to cache.

  val CHUNK_SIZE            = 8 * 1024
  val X_SENDRESOURCE_HEADER = "X-Sendresource"

  // See comment of X_SENDFILE_HEADER_IS_FROM_CONTROLLER
  val X_SENDRESOURCE_HEADER_IS_FROM_CONTROLLER = "X-Sendresource-Is-From-Controller"

  def setHeader(response: FullHttpResponse, path: String, fromController: Boolean) {
    HttpHeaders.setHeader(response, X_SENDRESOURCE_HEADER, path)
    if (fromController) HttpHeaders.setHeader(response, X_SENDRESOURCE_HEADER_IS_FROM_CONTROLLER, "true")
  }

  def isHeaderSet(response: FullHttpResponse) = response.headers.contains(X_SENDRESOURCE_HEADER)

  /** @return false if not found */
  def sendResource(
      ctx: ChannelHandlerContext, env: HandlerEnv, promise: ChannelPromise,
      request: HttpRequest, response: FullHttpResponse, path: String, noLog: Boolean)
  {
    val mimeo = Option(HttpHeaders.getHeader(response, CONTENT_TYPE))
    Etag.forResource(path, mimeo, Gzip.isAccepted(request)) match {
      case Etag.NotFound =>
        // Keep alive is handled by XSendFile
        XSendFile.set404Page(response, noLog)
        ctx.write(env, promise)

      case Etag.Small(bytes, etag, mimeo, gzipped) =>
        if (Etag.areEtagsIdentical(request, etag)) {
          response.setStatus(NOT_MODIFIED)
          response.content.clear()
        } else {
          Etag.set(response, etag)
          if (mimeo.isDefined) HttpHeaders.setHeader(response, CONTENT_TYPE, mimeo.get)
          if (gzipped)         HttpHeaders.setHeader(response, CONTENT_ENCODING, "gzip")

          if ((request.getMethod == HEAD || request.getMethod == OPTIONS) && response.getStatus == OK) {
            // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
            HttpHeaders.setContentLength(response, bytes.length)
            response.content.clear()
          } else {
            ByteBufUtil.writeComposite(response.content, Unpooled.wrappedBuffer(bytes))
          }
        }

        val channel = ctx.channel
        val future  = ctx.write(env, promise)
        NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
        if (!noLog) {
          val remoteAddress = channel.remoteAddress
          AccessLog.logResourceInJarAccess(remoteAddress, request, response)
        }
    }
  }
}

/**
 * This handler sends resource files (should be small) in classpath.
 */
@Sharable
class XSendResource extends ChannelOutboundHandlerAdapter {
  import XSendResource._

  override def write(ctx: ChannelHandlerContext, msg: Object, promise: ChannelPromise) {
    if (!msg.isInstanceOf[HandlerEnv]) {
      ctx.write(msg, promise)
      return
    }

    val env      = msg.asInstanceOf[HandlerEnv]
    val request  = env.request
    val response = env.response
    val path     = HttpHeaders.getHeader(response, X_SENDRESOURCE_HEADER)
    if (path == null) {
      ctx.write(env, promise)
      return
    }

    // Remove non-standard header to avoid leaking information
    HttpHeaders.removeHeader(response, X_SENDRESOURCE_HEADER)

    // See comment of X_SENDRESOURCE_HEADER_IS_FROM_CONTROLLER
    // Remove non-standard header to avoid leaking information
    val noLog = response.headers.contains(X_SENDRESOURCE_HEADER_IS_FROM_CONTROLLER)
    if (noLog) HttpHeaders.removeHeader(response, X_SENDRESOURCE_HEADER_IS_FROM_CONTROLLER)

    sendResource(ctx, env, promise, request, response, path, noLog)
  }
}
