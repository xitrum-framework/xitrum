package xt.server

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import org.jboss.netty.handler.codec.http.{HttpRequestDecoder, HttpChunkAggregator, HttpResponseEncoder}

import xt.middleware.RoutedApp

class ChannelPipelineFactory extends CPF {
  def getPipeline: ChannelPipeline = {
    val pipeline = Channels.pipeline
    val app      = new RoutedApp

    pipeline.addLast("decoder",    new HttpRequestDecoder)
    pipeline.addLast("aggregator", new HttpChunkAggregator(1024*1024))
    pipeline.addLast("encoder",    new HttpResponseEncoder)
    pipeline.addLast("handler",    new Handler(app))

    pipeline
  }
}
