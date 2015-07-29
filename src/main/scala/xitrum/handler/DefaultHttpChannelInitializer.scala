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
 *   Request2Env
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

  lazy val baseUrlRemover    = new BaseUrlRemover
  lazy val basicAuth         = new BasicAuth
  lazy val publicFileServer  = new PublicFileServer
  lazy val webJarsServer     = new WebJarsServer
  lazy val uriParser         = new UriParser
  lazy val methodOverrider   = new MethodOverrider
  lazy val dispatcher        = new Dispatcher
  lazy val badClientSilencer = new BadClientSilencer

  // Sharable outbound handlers

  lazy val setCORS           = new SetCORS
  lazy val OPTIONSResponse   = new OPTIONSResponse
  lazy val fixiOS6SafariPOST = new FixiOS6SafariPOST
  lazy val xSendFile         = new XSendFile
  lazy val xSendResource     = new XSendResource
  lazy val env2Response      = new Env2Response

  def removeUnusedHandlersForWebSocket(pipeline: ChannelPipeline) {
    // WebSocket handshaker in Netty dynamically changes the pipeline like this:
    // pipeline.remove(classOf[HttpChunkAggregator])
    // pipeline.replace(classOf[HttpRequestDecoder],  "wsdecoder", new WebSocket08FrameDecoder(true, this.allowExtensions))
    // pipeline.replace(classOf[HttpResponseEncoder], "wsencoder", new WebSocket08FrameEncoder(false))

    // Inbound

    removeHandlerIfExists(pipeline, classOf[Request2Env])
    removeHandlerIfExists(pipeline, classOf[BaseUrlRemover])
    if (Config.xitrum.basicAuth.isDefined)
    removeHandlerIfExists(pipeline, classOf[BasicAuth])
    removeHandlerIfExists(pipeline, classOf[PublicFileServer])
    removeHandlerIfExists(pipeline, classOf[WebJarsServer])
    removeHandlerIfExists(pipeline, classOf[UriParser])
    removeHandlerIfExists(pipeline, classOf[MethodOverrider])
    removeHandlerIfExists(pipeline, classOf[Dispatcher])

    // Do not remove BadClientSilencer; WebSocketEventDispatcher will be added
    // before BadClientSilencer, see WebSocketAction#acceptWebSocket
    //removeHandlerIfExists(pipeline, classOf[BadClientSilencer])

    // Outbound

    removeHandlerIfExists(pipeline, classOf[ChunkedWriteHandler])
    removeHandlerIfExists(pipeline, classOf[Env2Response])
    removeHandlerIfExists(pipeline, classOf[SetCORS])
    removeHandlerIfExists(pipeline, classOf[OPTIONSResponse])
    removeHandlerIfExists(pipeline, classOf[FixiOS6SafariPOST])
    removeHandlerIfExists(pipeline, classOf[XSendFile])
    removeHandlerIfExists(pipeline, classOf[XSendResource])
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
    p.addLast(classOf[FlashSocketPolicyHandler].getName, new FlashSocketPolicyHandler)

    p.addLast(classOf[HttpRequestDecoder].getName,       new HttpRequestDecoder(
                                                           Config.xitrum.request.maxInitialLineLength,
                                                           Config.xitrum.request.maxHeaderSize,
                                                           8192))
    p.addLast(classOf[Request2Env].getName,              new Request2Env)
    p.addLast(classOf[BaseUrlRemover].getName,           baseUrlRemover)

    if (Config.xitrum.basicAuth.isDefined)
    p.addLast(classOf[BasicAuth].getName,                basicAuth)

    p.addLast(classOf[PublicFileServer].getName,         publicFileServer)
    p.addLast(classOf[WebJarsServer].getName,            webJarsServer)
    p.addLast(classOf[UriParser].getName,                uriParser)
    p.addLast(classOf[MethodOverrider].getName,          methodOverrider)
    p.addLast(classOf[Dispatcher].getName,               dispatcher)
    p.addLast(classOf[BadClientSilencer].getName,        badClientSilencer)

    // Outbound

    p.addLast(classOf[HttpResponseEncoder].getName, new HttpResponseEncoder)
    p.addLast(classOf[ChunkedWriteHandler].getName, new ChunkedWriteHandler)  // For writing ChunkedFile, at XSendFile
    p.addLast(classOf[Env2Response].getName,        env2Response)
    p.addLast(classOf[SetCORS].getName,             setCORS)
    p.addLast(classOf[OPTIONSResponse].getName,     OPTIONSResponse)
    p.addLast(classOf[FixiOS6SafariPOST].getName,   fixiOS6SafariPOST)
    p.addLast(classOf[XSendFile].getName,           xSendFile)
    p.addLast(classOf[XSendResource].getName,       xSendResource)
  }
}
