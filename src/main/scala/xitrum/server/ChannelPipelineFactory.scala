package xitrum.server

import org.jboss.netty.channel.{StaticChannelPipeline, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}

import xitrum.Config
import xitrum.handler.up._
import xitrum.handler.down._
import xitrum.handler.updown._

/** See doc/HANDLER */
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
      // Downstream, direction: last handler -> first handler

      // Up
      new HttpRequestDecoder,
      new HttpChunkAggregator(maxRequestContentLengthInB),

      // Down
      new HttpResponseEncoder,

      // Both up and down
      new XSendfile,

      // Up
      new PublicFileServer,
      new PublicResourceServer,
      new Request2Env,
      new UriParser,
      new BodyParser,
      new MethodOverrider,
      new Dispatcher,

      // Down
      new Env2Response,
      new ResponseCacher)
  }
}
