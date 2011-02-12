package xt.server

import org.jboss.netty.channel.{StaticChannelPipeline, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}

import xt.Config
import xt.handler.up._
import xt.handler.down._

class ChannelPipelineFactory extends CPF {
  def getPipeline: ChannelPipeline = {
    // StaticChannelPipeline provides extreme performance at the cost of
    // disabled dynamic manipulation of pipeline
    new StaticChannelPipeline(
      // Upstream, direction: first handler -> last handler
      new HttpRequestDecoder,
      new HttpChunkAggregator(Config.maxContentLength),
      new PublicResourceServer,
      new Request2Env,
      new UriParser,
      new PublicFileServer,  // uses pathInfo parsed at UriParser
      new BodyParser,
      new MethodOverrider,   // uses _method parsed at BodyParser
      new Dispatcher,        // should be last

      // Downstream, direction: last handler -> first handler
      new HttpResponseEncoder,
      new Env2Response,
      new FileSender)
  }
}
