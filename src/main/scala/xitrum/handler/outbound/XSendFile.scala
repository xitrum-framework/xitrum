package xitrum.handler.outbound

import java.io.RandomAccessFile

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelOutboundHandlerAdapter, ChannelHandler, ChannelHandlerContext, ChannelFuture, ChannelPromise, DefaultFileRegion}
import io.netty.handler.codec.http.{FullHttpResponse, HttpMethod, HttpHeaderNames, HttpHeaderValues, HttpResponseStatus, HttpUtil, LastHttpContent}
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedFile
import ChannelHandler.Sharable
import HttpHeaderNames._
import HttpHeaderValues._
import HttpMethod._
import HttpResponseStatus._

import xitrum.etag.{Etag, NotModified}
import xitrum.handler.{AccessLog, HandlerEnv, NoRealPipelining}
import xitrum.util.{ByteBufUtil, Gzip}

// Based on https://github.com/netty/netty/tree/master/example/src/main/java/io/netty/example/http/file
object XSendFile {
  // setClientCacheAggressively should be called at PublicFileServer, not
  // here because XSendFile may be used by applications which does not want
  // to clients to cache.

  val CHUNK_SIZE        = 8 * 1024
  val X_SENDFILE_HEADER = "X-Sendfile"

  // To avoid duplicate log like this when X_SENDFILE_HEADER is set by action
  // GET /echo/ -> 404 (static)
  // GET /echo/ -> xitrum.sockjs.SockJsController#iframe, pathParams: {iframe: } -> 404, 1 [ms]
  val X_SENDFILE_HEADER_IS_FROM_ACTION = "X-Sendfile-Is-From-Action"

  private[this] val ABS_404 = xitrum.root + "/public/404.html"
  private[this] val ABS_500 = xitrum.root + "/public/500.html"

  /** @param path see Renderer#renderFile */
  def setHeader(response: FullHttpResponse, path: String, fromAction: Boolean) {
    response.headers.set(X_SENDFILE_HEADER, path)
    if (fromAction) response.headers.set(X_SENDFILE_HEADER_IS_FROM_ACTION, "true")
  }

  def isHeaderSet(response: FullHttpResponse) = response.headers.contains(X_SENDFILE_HEADER)

  /**
   * Removes non-standard headers, specific to this handler, to avoid leaking
   * information to the remote client.
   */
  def removeHeaders(response: FullHttpResponse) {
    response.headers.remove(X_SENDFILE_HEADER)
    response.headers.remove(X_SENDFILE_HEADER_IS_FROM_ACTION)
  }

  def set404Page(response: FullHttpResponse, fromController: Boolean) {
    response.setStatus(NOT_FOUND)
    setHeader(response, ABS_404, fromController)
  }

  def set500Page(response: FullHttpResponse, fromController: Boolean) {
    response.setStatus(INTERNAL_SERVER_ERROR)
    setHeader(response, ABS_500, fromController)
  }

  def sendFile(ctx: ChannelHandlerContext, env: HandlerEnv, promise: ChannelPromise) {
    val channel       = ctx.channel
    val remoteAddress = channel.remoteAddress
    val request       = env.request
    val response      = env.response
    val path          = response.headers.get(X_SENDFILE_HEADER)
    val mimeo         = Option(response.headers.get(CONTENT_TYPE))
    val noLog         = response.headers.contains(X_SENDFILE_HEADER_IS_FROM_ACTION)

    // Try to serve from cache.
    //
    // For big file, "the initial line and headers" will be sent by Env2Response
    // handler. The X_SENDFILE_HEADER is used to tell Env2Response to only send
    // headers, not a FullHttpResponse.
    Etag.forFile(path, mimeo, Gzip.isAccepted(request)) match {
      case Etag.NotFound =>
        removeHeaders(response)

        response.setStatus(NOT_FOUND)
        NotModified.setNoClientCache(response)

        if (path.startsWith(ABS_404)) {  // Even 404.html is not found!
          val future = ctx.write(env, promise)
          NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
          if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)
        } else {
          setHeader(response, ABS_404, fromAction = false)
          sendFile(ctx, env, promise)  // Recursive
        }

      case Etag.Small(bytes, etag, mmo, gzipped) =>
        removeHeaders(response)

        if (Etag.areEtagsIdentical(request, etag)) {
          response.setStatus(NOT_MODIFIED)
          response.content.clear()
        } else {
          Etag.set(response, etag)
          if (mmo.isDefined) response.headers.set(CONTENT_TYPE,     mmo.get)
          if (gzipped)       response.headers.set(CONTENT_ENCODING, "gzip")

          HttpUtil.setContentLength(response, bytes.length)
          if ((request.method == HEAD || request.method == OPTIONS) && response.status == OK) {
            // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
            response.content.clear()
          } else {
            ByteBufUtil.writeComposite(response.content, Unpooled.wrappedBuffer(bytes))
          }
        }
        val future = ctx.write(env, promise)
        NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
        if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)

      case Etag.TooBig(file, mmo) =>
        // LAST_MODIFIED is not reliable as ETAG when this is a cluster of web servers,
        // but it's still good to give it a try
        val lastModifiedRfc2822 = NotModified.formatRfc2822(file.lastModified)
        if (request.headers.get(IF_MODIFIED_SINCE) == lastModifiedRfc2822) {
          removeHeaders(response)

          response.setStatus(NOT_MODIFIED)
          response.content.clear()
          val future = ctx.write(env, promise)
          NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
          if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)
          return
        }

        val raf = new RandomAccessFile(path, "r")

        val (offset, length) = RangeParser.parse(request.headers.get(RANGE), raf.length) match {
          case UnsupportedRange =>
            (0L, raf.length)  // 0L is for avoiding "type mismatch" compile error

          case UnsatisfiableRange =>
            // A server sending a response with status code 416 (Requested range not satisfiable)
            // SHOULD include a Content-Range field with a byte-range-resp-spec of "*".
            // The instance-length specifies the current length of
            response.setStatus(REQUESTED_RANGE_NOT_SATISFIABLE)
            response.headers.set(ACCEPT_RANGES, BYTES)
            response.headers.set(CONTENT_RANGE, "bytes */" + raf.length)
            (0L, 0L)

          case SatisfiableRange(startIndex, endIndex) =>
            response.setStatus(PARTIAL_CONTENT)
            response.headers.set(ACCEPT_RANGES, BYTES)
            response.headers.set(CONTENT_RANGE, "bytes " + startIndex + "-" + endIndex + "/" + raf.length)
            (startIndex, endIndex - startIndex + 1)
        }

        HttpUtil.setContentLength(response, length)
        response.headers.set(LAST_MODIFIED, lastModifiedRfc2822)
        if (mmo.isDefined) response.headers.set(CONTENT_TYPE, mmo.get)
        if (!noLog) AccessLog.logStaticFileAccess(remoteAddress, request, response)

        if (response.status == REQUESTED_RANGE_NOT_SATISFIABLE || (request.method == HEAD && response.status == OK)) {
          removeHeaders(response)

          // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
          response.content.clear()

          ctx.write(env, promise)
          NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, promise)
          return
        }

        // Send the initial line and headers.
        // Do not pass promise here; it'll be completed below.
        ctx.write(env)

        if (ctx.pipeline.get(classOf[SslHandler]) != null) {
          // Cannot use zero-copy with HTTPS
          ctx
            .write(new ChunkedFile(raf, offset, length, CHUNK_SIZE))
            .addListener((_: ChannelFuture) => raf.close())
        } else {
          // No encryption - use zero-copy
          val region = new DefaultFileRegion(raf.getChannel, offset, length)
          ctx
            .write(region)  // region will automatically be released
            .addListener((_: ChannelFuture) => raf.close())
        }

        // Write the end marker
        val future = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT, promise)
        NoRealPipelining.if_keepAliveRequest_then_resumeReading_else_closeOnComplete(request, channel, future)
    }
  }
}

/**
 * This handler sends file:
 * 1. If the file is big: use zero-copy for HTTP or chunking for HTTPS
 * 2. If the file is small: cache in memory and use normal response
 */
@Sharable
class XSendFile extends ChannelOutboundHandlerAdapter {
  override def write(ctx: ChannelHandlerContext, msg: Object, promise: ChannelPromise) {
    if (!msg.isInstanceOf[HandlerEnv]) {
      ctx.write(msg, promise)
      return
    }

    val env      = msg.asInstanceOf[HandlerEnv]
    val response = env.response
    if (!XSendFile.isHeaderSet(response)) {
      ctx.write(env, promise)
      return
    }

    XSendFile.sendFile(ctx, env, promise)
  }
}
