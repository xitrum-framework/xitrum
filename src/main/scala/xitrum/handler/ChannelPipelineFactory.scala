package xitrum.handler

import io.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import io.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}
import io.netty.handler.execution.{ExecutionHandler, OrderedMemoryAwareThreadPoolExecutor}

import xitrum.Config
import xitrum.handler.up._
import xitrum.handler.down._
import xitrum.handler.updown._

object ChannelPipelineFactory {
  def removeUnusedDefaultHttpHandlersForWebSocket(pipeline: ChannelPipeline) {
    pipeline.remove(classOf[XSendFile])
    pipeline.remove(classOf[XSendResource])
    pipeline.remove(classOf[BaseUriRemover])
    pipeline.remove(classOf[PublicFileServer])
    pipeline.remove(classOf[PublicResourceServer])
    pipeline.remove(classOf[Request2Env])
    pipeline.remove(classOf[UriParser])
    pipeline.remove(classOf[BodyParser])
    pipeline.remove(classOf[MethodOverrider])
    pipeline.remove(classOf[Dispatcher])
    pipeline.remove(classOf[Env2Response])
    pipeline.remove(classOf[ResponseCacher])
  }
}

// See doc/sphinx/handler.rst
class ChannelPipelineFactory(https: Boolean) extends CPF {
  // Sharable handlers
  private val baseUriRemover       = new BaseUriRemover
  private val publicFileServer     = new PublicFileServer
  private val publicResourceServer = new PublicResourceServer
  private val request2Env          = new Request2Env
  private val uriParser            = new UriParser
  private val bodyParser           = new BodyParser
  private val methodOverrider      = new MethodOverrider
  private val env2Response         = new Env2Response
  private val responseCacher       = new ResponseCacher

  /*
    From Netty's documentation about ExecutionHandler:
      Used when your ChannelHandler
      performs a blocking operation that takes long time or accesses a resource
      which is not CPU-bound business logic such as DB access. Running such
      operations in a pipeline without an ExecutionHandler will result in
      unwanted hiccup during I/O because an I/O thread cannot perform I/O until
      your handler returns the control to the I/O thread.

    We put this right before Dispatcher to avoid request/response I/O hiccup.
  */
  private val executionHandler = {
    val corePoolSize         = Runtime.getRuntime.availableProcessors * Config.EXECUTIORS_PER_CORE
    val maxTotalMemorySize   = Runtime.getRuntime.maxMemory / 2
    val maxChannelMemorySize = maxTotalMemorySize
    new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(
          corePoolSize, maxChannelMemorySize, maxTotalMemorySize))
  }

  def getPipeline: ChannelPipeline = {
    val handlers1 = httpHandlers
    val handlers2 = if (https) ServerSsl.handler +: handlers1 else handlers1

    // StaticChannelPipeline provides extreme performance at the cost of
    // disabled dynamic manipulation of pipeline
    //
    // Creating StaticChannelPipeline with empty constructor will cause
    // java.lang.IllegalArgumentException: no handlers specified
    //new StaticChannelPipeline(handlers2:_*)

    // Cannot use StaticChannelPipeline because WebSocket handshaker in Netty
    // dynamically changes the pipeline like this:
    // pipeline.remove(classOf[HttpChunkAggregator])
    // pipeline.replace(classOf[HttpRequestDecoder], "wsdecoder", new WebSocket08FrameDecoder(true, this.allowExtensions))
    // pipeline.replace(classOf[HttpResponseEncoder], "wsencoder", new WebSocket08FrameEncoder(false))
    Channels.pipeline(handlers2:_*)
  }

  /**
   * You can override this method to customize the default pipeline.
   *
   * Upstream direction: first handler -> last handler
   * Downstream direction: last handler -> first handler
   */
  def httpHandlers = List(
    // Up
    new HttpRequestDecoder,
    new HttpChunkAggregator(Config.config.request.maxSizeInMB * 1024 * 1024),

    // Down
    new HttpResponseEncoder,

    // Both up and down
    new XSendFile,
    new XSendResource,

    // Up
    baseUriRemover,
    publicFileServer,
    publicResourceServer,

    request2Env,
    uriParser,
    bodyParser,
    methodOverrider,
    executionHandler,  // Must be shared
    new Dispatcher,

    // Down
    env2Response,
    responseCacher
  )
}
