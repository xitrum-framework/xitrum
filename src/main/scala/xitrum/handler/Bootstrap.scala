package xitrum.handler

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{ChannelInitializer, EventLoopGroup}
import io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

import xitrum.Config

object Bootstrap {
  def newBootstrap(channelInitializer: ChannelInitializer[SocketChannel]) =
    if (Config.xitrum.edgeTriggeredEpoll)
      newEpollBootstrap(channelInitializer)
    else
      newNioBootstrap(channelInitializer)

  private def newNioBootstrap(
    channelInitializer: ChannelInitializer[SocketChannel]
  ): (ServerBootstrap, Seq[EventLoopGroup]) =
  {
    val bossGroup   = new NioEventLoopGroup(1)
    val workerGroup = new NioEventLoopGroup
    val bootstrap   = new ServerBootstrap
    bootstrap.group(bossGroup, workerGroup)
             .channel(classOf[NioServerSocketChannel])
             .childHandler(channelInitializer)
    (bootstrap, Seq(bossGroup, workerGroup))
  }

  private def newEpollBootstrap(
      channelInitializer: ChannelInitializer[SocketChannel]
  ): (ServerBootstrap, Seq[EventLoopGroup]) =
  {
    val bossGroup   = new EpollEventLoopGroup(1)
    val workerGroup = new EpollEventLoopGroup
    val bootstrap   = new ServerBootstrap
    bootstrap.group(bossGroup, workerGroup)
             .channel(classOf[EpollServerSocketChannel])
             .childHandler(channelInitializer)
    (bootstrap, Seq(bossGroup, workerGroup))
  }
}
