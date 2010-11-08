package xt.server

import xt.Config
import xt.http_handler._

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}

class ChannelPipelineFactory(app: App) extends CPF {
  def getPipeline: ChannelPipeline = {
    val pipeline = Channels.pipeline

    // Upstream
    pipeline.addLast("decoder",    new HttpRequestDecoder)
    pipeline.addLast("aggregator", new HttpChunkAggregator(Config.maxContentLength))
    pipeline.addLast("netty2xt",   new Netty2Xt)
    pipeline.addLast("public",     new PublicHandler)

    // Downstream
    pipeline.addLast("encoder",   new HttpResponseEncoder)
    pipeline.addLast("xt2netty",  new Xt2Netty)
    pipeline.addLast("sendfile",  new SendfileHandler)

    pipeline
  }
}
