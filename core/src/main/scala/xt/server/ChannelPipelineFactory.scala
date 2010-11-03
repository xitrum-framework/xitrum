package xt.server

import xt.middleware.App

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}

class ChannelPipelineFactory(app: App) extends CPF {
  def getPipeline: ChannelPipeline = {
    val pipeline = Channels.pipeline

    pipeline.addLast("decoder",    new HttpRequestDecoder)
    pipeline.addLast("aggregator", new HttpChunkAggregator(1024*1024))
    pipeline.addLast("encoder",    new HttpResponseEncoder)
    pipeline.addLast("handler",    new Handler(app))

    pipeline
  }
}
