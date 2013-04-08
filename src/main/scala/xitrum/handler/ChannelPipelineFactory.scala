package xitrum.handler

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}
import org.jboss.netty.handler.execution.{ExecutionHandler, OrderedMemoryAwareThreadPoolExecutor}
import org.jboss.netty.handler.stream.ChunkedWriteHandler

import xitrum.Config
import xitrum.handler.up._
import xitrum.handler.down._

object ChannelPipelineFactory {
  def removeUnusedDefaultHttpHandlersForWebSocket(pipeline: ChannelPipeline) {
    // WebSocket handshaker in Netty dynamically changes the pipeline like this:
    // pipeline.remove(classOf[HttpChunkAggregator])
    // pipeline.replace(classOf[HttpRequestDecoder], "wsdecoder", new WebSocket08FrameDecoder(true, this.allowExtensions))
    // pipeline.replace(classOf[HttpResponseEncoder], "wsencoder", new WebSocket08FrameEncoder(false))

    pipeline.remove(classOf[NoPipelining])
    if (Config.xitrum.basicAuth.isDefined)
    pipeline.remove(classOf[BasicAuth])
    pipeline.remove(classOf[BaseUrlRemover])
    pipeline.remove(classOf[PublicFileServer])
    pipeline.remove(classOf[PublicResourceServer])
    pipeline.remove(classOf[Request2Env])
    pipeline.remove(classOf[UriParser])
    pipeline.remove(classOf[BodyParser])
    pipeline.remove(classOf[MethodOverrider])
    pipeline.remove(classOf[Dispatcher])

    pipeline.remove(classOf[ChunkedWriteHandler])
    pipeline.remove(classOf[FixiOS6SafariPOST])
    pipeline.remove(classOf[XSendFile])
    pipeline.remove(classOf[XSendResource])
    pipeline.remove(classOf[Env2Response])
    pipeline.remove(classOf[ResponseCacher])
  }
}

// See doc/sphinx/handler.rst
class ChannelPipelineFactory(https: Boolean) extends CPF {
  // Sharable handlers

  private[this] val noPipelining         = new NoPipelining
  private[this] val basicAuth            = new BasicAuth
  private[this] val baseUrlRemover       = new BaseUrlRemover
  private[this] val publicFileServer     = new PublicFileServer
  private[this] val publicResourceServer = new PublicResourceServer
  private[this] val request2Env          = new Request2Env
  private[this] val uriParser            = new UriParser
  private[this] val bodyParser           = new BodyParser
  private[this] val methodOverrider      = new MethodOverrider
  private[this] val dispatcher           = new Dispatcher

  private[this] val fixiOS6SafariPOST    = new FixiOS6SafariPOST
  private[this] val xSendFile            = new XSendFile
  private[this] val xSendResource        = new XSendResource
  private[this] val env2Response         = new Env2Response
  private[this] val responseCacher       = new ResponseCacher

  /**
   * You can override this method to customize the default pipeline.
   *
   * Upstream direction: first handler -> last handler
   * Downstream direction: last handler -> first handler
   */
  def getPipeline: ChannelPipeline = {
    // This method is run for every request, thus should be fast

    val ret = Channels.pipeline()

    // Up
    if (https)
    ret.addLast("SSL",                  ServerSsl.handler())
    ret.addLast("HttpRequestDecoder",   new HttpRequestDecoder)
    ret.addLast("HttpChunkAggregator",  new HttpChunkAggregator(Config.xitrum.request.maxSizeInMB * 1024 * 1024))
    ret.addLast("noPipelining",         noPipelining)
    if (Config.xitrum.basicAuth.isDefined)
    ret.addLast("basicAuth",            basicAuth)
    ret.addLast("baseUrlRemover",       baseUrlRemover)  // HttpRequest is attached to the channel here
    ret.addLast("publicFileServer",     publicFileServer)
    ret.addLast("publicResourceServer", publicResourceServer)
    ret.addLast("request2Env",          request2Env)
    ret.addLast("uriParser",            uriParser)
    ret.addLast("bodyParser",           bodyParser)
    ret.addLast("methodOverrider",      methodOverrider)
    ret.addLast("dispatcher",           dispatcher)

    // Down
    ret.addLast("HttpResponseEncoder", new HttpResponseEncoder)
    ret.addLast("ChunkedWriteHandler", new ChunkedWriteHandler)  // For writing ChunkedFile, at XSendFile
    ret.addLast("fixiOS6SafariPOST",   fixiOS6SafariPOST)
    ret.addLast("xSendFile",           xSendFile)
    ret.addLast("xSendResource",       xSendResource)
    ret.addLast("env2Response",        env2Response)
    ret.addLast("responseCacher",      responseCacher)

    ret
  }
}
