package xt.server

import xt.Config
import xt.handler.up._
import xt.handler.down._

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}

class ChannelPipelineFactory extends CPF {
  def getPipeline: ChannelPipeline = {
    val pipeline = Channels.pipeline

    // Upstream, direction: first handler -> last handler
    pipeline.addLast("decoder",    new HttpRequestDecoder)
    pipeline.addLast("aggregator", new HttpChunkAggregator(Config.maxContentLength))
    pipeline.addLast("netty2xt",   new Netty2XtHandler)
    pipeline.addLast("public",     new PublicHandler)

    // Downstream, direction: last handler -> first handler
    pipeline.addLast("encoder",  new HttpResponseEncoder)
    pipeline.addLast("xt2netty", new Xt2NettyHandler)
    pipeline.addLast("sendfile", new SendfileHandler)

    pipeline
  }
}
