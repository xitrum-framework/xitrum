package xitrum.handler

import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.socket.SocketChannel
import io.netty.handler.ssl.{SslContext, SslContextBuilder, SslHandler, SslProvider}
import xitrum.{Config, Log}
import xitrum.handler.inbound.FlashSocketPolicyHandler
import xitrum.util.FileMonitor

import java.util.concurrent.atomic.AtomicLong

object SslChannelInitializer {
  var sslContext: SslContext = buildSslContext()
  rebuildSslContextOnCertModification()

  // Rebuild the SslContext after both certChainFile and keyFile have been modified.
  //
  // TODO https://github.com/xitrum-framework/xitrum/issues/692 Also rebuild the SslContext after the config file xitrum.conf has been modified to point to new paths of certChainFile and keyFile
  private def rebuildSslContextOnCertModification(): Unit = {
    val https = Config.xitrum.https.get

    val certChainFileModifyCount = new AtomicLong()
    val keyFileModifyCount = new AtomicLong()

    FileMonitor.monitor(https.certChainFile.toPath) { (event, _, _) =>
      if (event == FileMonitor.MODIFY) {
        SslChannelInitializer.synchronized {
          Log.warn(s"certChainFile modified: ${https.certChainFile}")
          if (certChainFileModifyCount.incrementAndGet() == keyFileModifyCount.get()) {
            sslContext = buildSslContext()
            Log.warn("SslContext rebuilt")
          }
        }
      }
    }

    FileMonitor.monitor(https.keyFile.toPath) { (event, _, _) =>
      if (event == FileMonitor.MODIFY) {
        SslChannelInitializer.synchronized {
          Log.warn(s"keyFile modified: ${https.keyFile}")
          if (keyFileModifyCount.incrementAndGet() == certChainFileModifyCount.get()) {
            sslContext = buildSslContext()
            Log.warn("SslContext rebuilt")
          }
        }
      }
    }
  }

  private def buildSslContext(): SslContext = {
    val https = Config.xitrum.https.get
    val provider = if (https.openSSL) SslProvider.OPENSSL else SslProvider.JDK
    SslContextBuilder
      .forServer(https.certChainFile, https.keyFile)
      .sslProvider(provider)
      .build()
  }
}

/** This is a wrapper. It prepends SSL handler to the non-SSL pipeline. */
@Sharable
class SslChannelInitializer(nonSslChannelInitializer: ChannelInitializer[SocketChannel]) extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline
    p.addLast(classOf[SslHandler].getName, SslChannelInitializer.sslContext.newHandler(ch.alloc))
    p.addLast(nonSslChannelInitializer)

    // FlashSocketPolicyHandler can't be used with SSL
    DefaultHttpChannelInitializer.removeHandlerIfExists(p, classOf[FlashSocketPolicyHandler])
  }
}
