package xitrum.handler

import org.jboss.netty.channel.{Channels, ChannelPipeline, ChannelPipelineFactory => CPF}
import xitrum.handler.up.{FlashSocketPolicyRequestDecoder, FlashSocketPolicyResponseSender}

class FlashSocketChannelPipelineFactory extends CPF {
  private[this] val flashSocketPolicyResponseSender = new FlashSocketPolicyResponseSender

  def getPipeline: ChannelPipeline = {
    Channels.pipeline(
      new FlashSocketPolicyRequestDecoder,
      flashSocketPolicyResponseSender
    )
  }
}
