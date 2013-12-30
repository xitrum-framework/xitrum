//package xitrum.handler.up
//
//import org.jboss.netty.buffer.ChannelBuffers
//import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent}
//import ChannelHandler.Sharable
//
//import xitrum.handler.AccessLog
//import xitrum.util.Loader
//
//object FlashSocketPolicyResponseSender {
//  val RESPONSE = ChannelBuffers.wrappedBuffer(Loader.bytesFromClasspath("flash_socket_policy.xml"))
//}
//
//@Sharable
//class FlashSocketPolicyResponseSender extends SimpleChannelUpstreamHandler with BadClientSilencer {
//  import FlashSocketPolicyResponseSender._
//
//  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
//    val channel = ctx.getChannel
//    channel.write(RESPONSE)
//    AccessLog.logFlashSocketPolicyFileAccess(channel.getRemoteAddress)
//  }
//}
