package xt.server

import xt.Config
import xt.handler.up._
import xt.handler.down._

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}

class ChannelPipelineFactory extends CPF {
  def getPipeline: ChannelPipeline = {
    Channels.pipeline(
      // Upstream, direction: first handler -> last handler
      new HttpRequestDecoder,
      new HttpChunkAggregator(Config.maxContentLength),
      new Netty2XtConverter,

      new ParamsParser,
      new MethodOverrider,
      new PublicFileServer,  // uses pathInfo parsed at ParamsParser
      new Dispatcher,        // Should be last

      // Downstream, direction: last handler -> first handler
      new HttpResponseEncoder,
      new Xt2NettyConverter,
      new CookieEncoder,
      new FileSender)
  }
}
