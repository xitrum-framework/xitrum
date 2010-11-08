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
      new Netty2XtHandler,
      new PublicHandler,
      new ParamsParser,
      new MethodOverride,
      new CookieDecoder,
      new SessionRestorer,

      // Downstream, direction: last handler -> first handler
      new HttpResponseEncoder,
      new Xt2NettyHandler,
      new CookieEncoder,
      new SessionStorer,
      new SendfileHandler)
  }
}
