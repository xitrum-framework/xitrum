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
    val corePoolSize         = Runtime.getRuntime.availableProcessors * 2 + 1
    val maxTotalMemorySize   = Runtime.getRuntime.maxMemory / 2
    val maxChannelMemorySize = maxTotalMemorySize
    new ExecutionHandler(
      new OrderedMemoryAwareThreadPoolExecutor(corePoolSize, maxChannelMemorySize, maxTotalMemorySize))
  }

  def getPipeline: ChannelPipeline = {
    // StaticChannelPipeline provides extreme performance at the cost of
    // disabled dynamic manipulation of pipeline
    //
    // Creating StaticChannelPipeline with empty constructor will cause
    // java.lang.IllegalArgumentException: no handlers specified

    // TODO: websocket

    new StaticChannelPipeline(
      // Upstream, direction: first handler -> last handler
      // Downstream, direction: last handler -> first handler

      // Up
      new HttpRequestDecoder,
      new HttpChunkAggregator(Config.maxRequestContentLengthInMB * 1024 * 1024),

      // Down
      new HttpResponseEncoder,

      // Both up and down
      new XSendFile,

      // Up
      new PublicFileServer,
      new Request2Env,
      new UriParser,
      new BodyParser,
      new MethodOverrider,
      executionHandler,  // Must be shared
      new Dispatcher,

      // Down
      new Env2Response,
      new ResponseCacher)
  }
}
