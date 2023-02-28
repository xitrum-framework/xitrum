package xitrum.handler

import better.files.FileExtensions
import io.netty.buffer.ByteBufAllocator
import io.netty.handler.ssl.{SslContext, SslContextBuilder, SslHandler, SslProvider}

import java.time.{Duration, Instant}

import xitrum.{Config, Log}

// The SSL certificate files specified in xitrum.conf are loaded and cached for 1h.
// After this time, if the files have been modified, they will be reloaded.
// Usually, SSL certificate files are renewed a few days/weeks before the expiration date, so this solution is OK.
//
// Notes:
// - We don't reload immediately after the files modification, as FileMonitor.monitor doesn't support symlink:
//   https://github.com/gmethvin/directory-watcher/issues/30
// - After Xitrum server is started, if you modify the file paths in xitrum.conf, they won't be picked up:
//   TODO https://github.com/xitrum-framework/xitrum/issues/692 Also rebuild the SslContext after the config file xitrum.conf has been modified to point to new paths of certChainFile and keyFile
object RebuilableSslContext {
  case class BuiltSslContext(sslContext: SslContext, rebuildableAfter: Instant, certFilesChecksum: String)

  private val CACHE_DURATION = Duration.ofHours(1)

  @volatile
  private var builtSslContext = buildSslContext(true)

  def newHandler(alloc: ByteBufAllocator): SslHandler = {
    val now = Instant.now()

    // Quick check, before going into the synchronized block (bottleneck)
    if (now.isAfter(builtSslContext.rebuildableAfter)) {
      RebuilableSslContext.synchronized {
        // Quick check again, rebuildableAfter may have been updated by other thread
        if (now.isAfter(builtSslContext.rebuildableAfter)) {
          // Slower check
          val checksum = certFilesChecksum()
          if (builtSslContext.certFilesChecksum != checksum) {
            builtSslContext = buildSslContext(false)
          } else {
            // Extend rebuildableAfter, so that we won't have to do the slower check above again very soon
            builtSslContext = extendSslContext(now)
          }
        }
      }
    }

    builtSslContext.sslContext.newHandler(alloc)
  }

  private def buildSslContext(firstBuild: Boolean): BuiltSslContext = {
    val https = Config.xitrum.https.get

    val provider = if (https.openSSL) SslProvider.OPENSSL else SslProvider.JDK
    val sslContext = SslContextBuilder
      .forServer(https.certChainFile, https.keyFile)
      .sslProvider(provider)
      .build()

    val rebuildableAfter = Instant.now().plus(CACHE_DURATION)
    if (firstBuild) {
      Log.info(s"SSL certificate files ${https.certChainFile} and ${https.keyFile} loaded, they will be reloadable in $CACHE_DURATION, after $rebuildableAfter")
    } else {
      Log.warn(s"SSL certificate files ${https.certChainFile} and ${https.keyFile} reloaded, they will be reloadable in $CACHE_DURATION, after $rebuildableAfter")
    }

    val checksum = certFilesChecksum()
    BuiltSslContext(sslContext, rebuildableAfter, checksum)
  }

  private def extendSslContext(now: Instant): BuiltSslContext = {
    val https = Config.xitrum.https.get

    val rebuildableAfter = now.plus(CACHE_DURATION)
    Log.warn(s"SSL certificate files ${https.certChainFile} and ${https.keyFile} not modified, they will be reloadable in $CACHE_DURATION, after $rebuildableAfter")
    builtSslContext.copy(rebuildableAfter = rebuildableAfter)
  }

  private def certFilesChecksum(): String = {
    val https = Config.xitrum.https.get
    https.certChainFile.toScala.md5 + "-" + https.keyFile.toScala.md5
  }
}
