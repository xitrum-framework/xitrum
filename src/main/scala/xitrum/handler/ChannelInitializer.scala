package xitrum.handler

import io.netty.channel.{ChannelHandler, ChannelInitializer, ChannelPipeline}
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.socket.SocketChannel

import io.netty.handler.codec.http.{HttpRequestDecoder, HttpResponseEncoder}
import io.netty.handler.stream.ChunkedWriteHandler

import xitrum.Config
import xitrum.handler.inbound._
import xitrum.handler.outbound._

/**
 * Sharable handlers are put here so that they can be easily picked up by apps
 * that want to use custom pipeline. Those apps may only want a subset of
 * default handlers.
 *
 * When an app uses its own dispatcher (not Xitrum's routing/dispatcher) and
 * only needs Xitrum's fast static file serving, it may use only these handlers:
 *
 * Inbound:
 *   HttpRequestDecoder
 *   BodyParser
 *   NoPipelining
 *   PublicFileServer
 *   Its own dispatcher
 *
 * Outbound:
 *   HttpResponseEncoder
 *   ChunkedWriteHandler
 *   XSendFile
 */
object DefaultHttpChannelInitializer {
  // Sharable inbound handlers

  lazy val noPipelining         = new NoPipelining
  lazy val baseUrlRemover       = new BaseUrlRemover
  lazy val basicAuth            = new BasicAuth
  lazy val publicFileServer     = new PublicFileServer
  lazy val publicResourceServer = new PublicResourceServer
  lazy val uriParser            = new UriParser
  lazy val methodOverrider      = new MethodOverrider
  lazy val dispatcher           = new Dispatcher

  // Sharable outbound handlers

  lazy val setCORS              = new SetCORS
  lazy val OPTIONSResponse      = new OPTIONSResponse
  lazy val fixiOS6SafariPOST    = new FixiOS6SafariPOST
  lazy val xSendFile            = new XSendFile
  lazy val xSendResource        = new XSendResource
  lazy val env2Response         = new Env2Response
  lazy val responseCacher       = new ResponseCacher

  def removeUnusedHandlersForWebSocket(pipeline: ChannelPipeline) {
    // WebSocket handshaker in Netty dynamically changes the pipeline like this:
    // pipeline.remove(classOf[HttpChunkAggregator])
    // pipeline.replace(classOf[HttpRequestDecoder],  "wsdecoder", new WebSocket08FrameDecoder(true, this.allowExtensions))
    // pipeline.replace(classOf[HttpResponseEncoder], "wsencoder", new WebSocket08FrameEncoder(false))

    // Inbound

    removeHandlerIfExists(pipeline, classOf[BodyParser])
    removeHandlerIfExists(pipeline, classOf[NoPipelining])
    removeHandlerIfExists(pipeline, classOf[BaseUrlRemover])
    if (Config.xitrum.basicAuth.isDefined)
    removeHandlerIfExists(pipeline, classOf[BasicAuth])
    removeHandlerIfExists(pipeline, classOf[PublicFileServer])
    removeHandlerIfExists(pipeline, classOf[PublicResourceServer])
    removeHandlerIfExists(pipeline, classOf[UriParser])
    removeHandlerIfExists(pipeline, classOf[MethodOverrider])
    removeHandlerIfExists(pipeline, classOf[Dispatcher])

    // Outbound

    removeHandlerIfExists(pipeline, classOf[ChunkedWriteHandler])
    removeHandlerIfExists(pipeline, classOf[Env2Response])
    removeHandlerIfExists(pipeline, classOf[SetCORS])
    removeHandlerIfExists(pipeline, classOf[OPTIONSResponse])
    removeHandlerIfExists(pipeline, classOf[FixiOS6SafariPOST])
    removeHandlerIfExists(pipeline, classOf[XSendFile])
    removeHandlerIfExists(pipeline, classOf[XSendResource])
    removeHandlerIfExists(pipeline, classOf[ResponseCacher])
  }

  /**
   * ChannelPipeline#remove(handler) throws exception if the handler does not
   * exist in the pipeline.
   */
  def removeHandlerIfExists(pipeline: ChannelPipeline, klass: Class[_ <: ChannelHandler]) {
    val handler = pipeline.get(klass)
    if (handler != null) pipeline.remove(handler)
  }
}

@Sharable
class DefaultHttpChannelInitializer extends ChannelInitializer[SocketChannel] {
  import DefaultHttpChannelInitializer._

  /**
   * You can override this method to customize the default pipeline.
   *
   * Inbound direction: first handler -> last handler
   * Outbound direction: last handler -> first handler
   */
  override def initChannel(ch: SocketChannel) {
    // This method is run for every request, thus should be fast

    val p          = ch.pipeline
    val portConfig = Config.xitrum.port

    // Inbound

    if (portConfig.flashSocketPolicy.isDefined && portConfig.flashSocketPolicy == portConfig.http)
    p.addLast("FlashSocketPolicyHandler", new FlashSocketPolicyHandler)
    p.addLast("HttpRequestDecoder",       new HttpRequestDecoder(Config.xitrum.request.maxInitialLineLength, 8192, 8192) with BadClientSilencer)
    p.addLast("BodyParser",               new BodyParser)      // Request is converted to HandlerEnv here
    p.addLast("NoPipelining",             noPipelining)
    p.addLast("BaseUrlRemover",           baseUrlRemover)
    if (Config.xitrum.basicAuth.isDefined)
    p.addLast("BasicAuth",                basicAuth)
    p.addLast("PublicFileServer",         publicFileServer)
    p.addLast("PublicResourceServer",     publicResourceServer)
    p.addLast("UriParser",                uriParser)
    p.addLast("MethodOverrider",          methodOverrider)
    p.addLast("Dispatcher",               dispatcher)

    // Outbound

    p.addLast("HttpResponseEncoder", new HttpResponseEncoder)
    p.addLast("ChunkedWriteHandler", new ChunkedWriteHandler)  // For writing ChunkedFile, at XSendFile
    p.addLast("Env2Response",        env2Response)
    p.addLast("SetCORS",             setCORS)
    p.addLast("OPTIONSResponse",     OPTIONSResponse)
    p.addLast("FixiOS6SafariPOST",   fixiOS6SafariPOST)
    p.addLast("XSendFile",           xSendFile)
    p.addLast("XSendResource",       xSendResource)
    p.addLast("ResponseCacher",      responseCacher)
  }
}

/** This is a wrapper. It prepends SSL handler to the non-SSL pipeline. */
@Sharable
class SslChannelInitializer(nonSslChannelInitializer: ChannelInitializer[SocketChannel]) extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel) {
    val p = ch.pipeline()
    p.addLast("SSL",    ServerSsl.handler())
    p.addLast("nonSsl", nonSslChannelInitializer)

    // FlashSocketPolicyHandler can't be used with SSL
    DefaultHttpChannelInitializer.removeHandlerIfExists(p, classOf[FlashSocketPolicyHandler])
  }
}
