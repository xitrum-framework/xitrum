package xitrum.handler.down

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.channel.{ChannelHandler, SimpleChannelDownstreamHandler, ChannelHandlerContext, MessageEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpMethod, HttpRequest, HttpResponse, HttpResponseStatus}
import ChannelHandler.Sharable
import HttpHeaders.Names._
import HttpMethod._
import HttpResponseStatus._

import xitrum.Config
import xitrum.etag.Etag
import xitrum.handler.HandlerEnv
import xitrum.util.{ChannelBufferToBytes, Gzip, Mime}
import xitrum.etag.NotModified

@Sharable
class Env2Response extends SimpleChannelDownstreamHandler {
  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendDownstream(e)
      return
    }

    val env      = m.asInstanceOf[HandlerEnv]
    val request  = env.request
    val response = env.response
    val future   = e.getFuture

    if (request.getMethod == HEAD && response.getStatus == OK)
      // http://stackoverflow.com/questions/3854842/content-length-header-with-head-requests
      response.setContent(ChannelBuffers.EMPTY_BUFFER)
    else if (!tryEtag(request, response))
      Gzip.tryCompressBigTextualResponse(request, response)

    setCORS(env)

    // Keep alive, channel reading resuming/closing etc. are handled
    // by the code that sends the response (Responder#respond)
    Channels.write(ctx, future, response)
  }

  //----------------------------------------------------------------------------

  /**
   * This does not make the server faster, but decreases the response transmission
   * time through the network to the browser.
   *
   * Only effective for non-empty non-async dynamic response,
   * e.g. not for static file (has alredy been handled and does not go through
   * this handler) or X-SendFile response (empty dynamic response).
   *
   * If HttpHeaders.getContentLength(response) != response.getContent.readableBytes,
   * it is because the response is sent in async mode.
   *
   * @return true if the NO_MODIFIED response is set by this method
   */
  private def tryEtag(request: HttpRequest, response: HttpResponse): Boolean = {
    if (response.containsHeader(CACHE_CONTROL) &&
        response.getHeader(CACHE_CONTROL).toLowerCase.contains("no-cache"))
      return false

    if (response.getStatus != OK)
      return false

    val contentLengthInHeader = HttpHeaders.getContentLength(response)
    val channelBuffer         = response.getContent
    if (contentLengthInHeader == 0 || contentLengthInHeader != channelBuffer.readableBytes) return false

    // No need to calculate ETag if it has been set, e.g. by the controller
    val etag1 = response.getHeader(ETAG)
    if (etag1 != null) {
      compareAndSetETag(request, response, etag1)
    } else {
      // It's not useful to calculate ETag for big response
      if (channelBuffer.readableBytes > Config.xitrum.staticFile.maxSizeInKBOfCachedFiles * 1024) return false

      val etag2 = Etag.forBytes(ChannelBufferToBytes(channelBuffer))
      compareAndSetETag(request, response, etag2)
    }
  }

  private def compareAndSetETag(request: HttpRequest, response: HttpResponse, etag: String): Boolean = {
    if (Etag.areEtagsIdentical(request, etag)) {
      // Only send headers, the response content is set to empty
      // (decrease response transmission time)
      response.setStatus(NOT_MODIFIED)
      response.removeHeader(CONTENT_TYPE)  // http://sockjs.github.com/sockjs-protocol/sockjs-protocol-0.3.3.html#section-25
      HttpHeaders.setContentLength(response, 0)
      response.setContent(ChannelBuffers.EMPTY_BUFFER)
      true
    } else {
      Etag.set(response, etag)
      false
    }
  }

  /** Set default CORS setting for the whole site. */
  private def setCORS(env: HandlerEnv) {
    if (Config.xitrum.response.corsAllowOrigins.isEmpty) return

    val corsAllowOrigins = Config.xitrum.response.corsAllowOrigins.get

    val request  = env.request
    val response = env.response

    // Access-Control-Max-Age
    if (env.request.getMethod == OPTIONS)
      NotModified.setClientCacheAggressively(response)

    val requestOrigin = request.getHeader(ORIGIN)

    // Access-Control-Allow-Origin
    if (!response.containsHeader(ACCESS_CONTROL_ALLOW_ORIGIN)) {
      if (corsAllowOrigins(0).equals("*")) {
        if (requestOrigin == null || requestOrigin == "null") {
          response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*")
        } else {
          response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
        }
      } else {
        if (corsAllowOrigins.contains(requestOrigin)) {
          response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin)
        } else {
          response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, corsAllowOrigins.mkString(", "))
        }
      }
    }

    // Access-Control-Allow-Credentials
    if (!response.containsHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS)) {
      response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, true)
    }

    // Access-Control-Allow-Methods
    if (env.route != null) {
      val allowMethods =
        OPTIONS.getName + ", " +
        Config.routes.corsAllowMethods.get(env.route.klass).get
      response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, allowMethods)
    }

    // Access-Control-Allow-Headers
    val accessControlRequestHeaders = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS)
    if (accessControlRequestHeaders != null)
      response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, accessControlRequestHeaders)
  }
}
