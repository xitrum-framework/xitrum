package xitrum.server

import org.jboss.netty.channel.{StaticChannelPipeline, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}

import xitrum.Config
import xitrum.handler.up._
import xitrum.handler.down._

class ChannelPipelineFactory extends CPF {
  def getPipeline: ChannelPipeline = {
    // StaticChannelPipeline provides extreme performance at the cost of
    // disabled dynamic manipulation of pipeline

    // Creating StaticChannelPipeline with empty constructor will cause
    // java.lang.IllegalArgumentException: no handlers specified

    //TODO websocket

    val maxRequestContentLengthInB = Config.maxRequestContentLengthInMB * 1024 * 1024
    new StaticChannelPipeline(
      // Upstream, direction: first handler -> last handler
      new HttpRequestDecoder,
      new HttpChunkAggregator(maxRequestContentLengthInB),
      new PublicResourceServer,
      new Request2Env,
      new UriParser,
      new PublicFileServer,
      new BodyParser,
      new MethodOverrider,
      new Dispatcher,  // Should be last

      // Downstream, direction: last handler -> first handler
      new HttpResponseEncoder,
      new Env2Response,
      new FileSender,
      new ResponseCacher)
  }
}
