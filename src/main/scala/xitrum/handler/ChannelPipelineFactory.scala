package xitrum.handler

import org.jboss.netty.channel.{StaticChannelPipeline, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}
import org.jboss.netty.handler.execution.{ExecutionHandler, OrderedMemoryAwareThreadPoolExecutor}

import xitrum.Config
import xitrum.handler.up._
import xitrum.handler.down._
import xitrum.handler.updown._

// See doc/sphinx/handler.rst
class ChannelPipelineFactory(https: Boolean) extends CPF {
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

  def getPipeline: ChannelPipeline = {
    val handlers1 = httpHandlers
    val handlers2 = if (https) ServerSsl.handler +: handlers1 else handlers1

    // StaticChannelPipeline provides extreme performance at the cost of
    // disabled dynamic manipulation of pipeline
    //
    // Creating StaticChannelPipeline with empty constructor will cause
    // java.lang.IllegalArgumentException: no handlers specified
    new StaticChannelPipeline(handlers2:_*)
  }

  // Upstream direction: first handler -> last handler
  // Downstream direction: last handler -> first handler
  def httpHandlers = {
    List(
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
}
