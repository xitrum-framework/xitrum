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

      new UriParser,
      new PublicFileServer,  // uses pathInfo parsed at UriParser
      new BodyParser,
      new MethodOverrider,   // uses _method parsed at BodyParser
      new Dispatcher,        // should be last

      // Downstream, direction: last handler -> first handler
      new HttpResponseEncoder,
      new FileSender,
      new Xt2NettyConverter)
  }
}
