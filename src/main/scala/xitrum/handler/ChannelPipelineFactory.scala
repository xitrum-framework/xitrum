package xitrum.handler

import org.jboss.netty.channel.{StaticChannelPipeline, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}
import org.jboss.netty.handler.execution.{ExecutionHandler, OrderedMemoryAwareThreadPoolExecutor}

import xitrum.Config
import xitrum.handler.up._
import xitrum.handler.down._
import xitrum.handler.updown._

/** See doc/HANDLER */
class ChannelPipelineFactory extends CPF {
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
    // In case of CPU bound, the pool size should be equal the number of cores
    // http://grizzly.java.net/nonav/docs/docbkx2.0/html/bestpractices.html
    val corePoolSize = Runtime.getRuntime.availableProcessors * 20

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
    // StaticChannelPipeline provides extreme performance at the cost of
    // disabled dynamic manipulation of pipeline
    //
    // Creating StaticChannelPipeline with empty constructor will cause
    // java.lang.IllegalArgumentException: no handlers specified

    // TODO: websocket

    new StaticChannelPipeline(
      // Upstream direction: first handler -> last handler
      // Downstream direction: last handler -> first handler

      // Up
      new HttpRequestDecoder,
      new HttpChunkAggregator(Config.maxRequestContentLengthInMB * 1024 * 1024),

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
      responseCacher)
  }
}
