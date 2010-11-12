package xt.handler.down

import xt.{Config, Logger}

import java.io.File
import java.io.RandomAccessFile

import org.jboss.netty.channel.{SimpleChannelDownstreamHandler, Channels, ChannelHandlerContext, MessageEvent, ChannelFuture, DefaultFileRegion, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpHeaders, HttpResponseStatus, HttpVersion}
import HttpResponseStatus._
import HttpVersion._
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.buffer.ChannelBuffers

import net.sf.ehcache.{CacheManager, Element}

object FileSender {
  val CHUNK_SIZE = 8192
}

/**
 * This handler send file to client using various strategies:
 * 1. If the file is big: use zero-copy for HTTP or chunking for HTTPS
 * 2. If the file is small: cache in memory and use normal response
 *
 * Cache is configureed by files_ehcache_name and files_max_size in xitrum.properties.
 */
class FileSender extends SimpleChannelDownstreamHandler with Logger {
  import FileSender._

  override def writeRequested(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpResponse]) {
      ctx.sendDownstream(e)
      return
    }

    val response = m.asInstanceOf[HttpResponse]

    if (!response.containsHeader("X-Sendfile")) {
      Channels.write(ctx, e.getFuture, response)
      return
    }

    // X-Sendfile is not standard
    // To avoid leaking the information, we remove it
    val abs = response.getHeader("X-Sendfile")
    response.removeHeader("X-Sendfile")

    val file = new File(abs)
    if (!file.exists() || !file.isFile()) {
      response.setStatus(NOT_FOUND)
      HttpHeaders.setContentLength(response, 0)
      Channels.write(ctx, e.getFuture, response)
      return
    }

    // Try to serve from cache
    val cache = CacheManager.getInstance.getCache(Config.filesEhcacheName)
    val elemKey = abs
    val elem = cache.get(elemKey)
    if (elem != null) {
      logger.debug("Serve " + abs + " from cache")
      val bytes = elem.getObjectValue.asInstanceOf[Array[Byte]]
      HttpHeaders.setContentLength(response, bytes.length)
      response.setContent(ChannelBuffers.wrappedBuffer(bytes))
      Channels.write(ctx, e.getFuture, response)
      return
    }

    var raf: RandomAccessFile = null
    try {
      raf = new RandomAccessFile(file, "r")
    } catch {
      case _ =>
        response.setStatus(NOT_FOUND)
        HttpHeaders.setContentLength(response, 0)
        Channels.write(ctx, e.getFuture, response)
        return
    }

    val fileLength = raf.length

    // Cache if the file is small
    if (Config.isProductionMode && fileLength <= Config.filesMaxSize) {
      val bytes = new Array[Byte](fileLength.toInt)
      raf.read(bytes)
      cache.put(new Element(elemKey, bytes))
      raf.close

      HttpHeaders.setContentLength(response, fileLength)
      response.setContent(ChannelBuffers.wrappedBuffer(bytes))
      Channels.write(ctx, e.getFuture, response)
      return
    }

    // OK, for big files we do not cache

    // Write the initial line and the header
    HttpHeaders.setContentLength(response, fileLength)
    Channels.write(ctx, e.getFuture, response)

    // Write the content
    if (ctx.getPipeline.get(classOf[SslHandler]) != null) {
      // Cannot use zero-copy with HTTPS
      Channels.write(ctx, e.getFuture, new ChunkedFile(raf, 0, fileLength, CHUNK_SIZE))
    } else {
      // No encryption - use zero-copy
      val region = new DefaultFileRegion(raf.getChannel, 0, fileLength)

      // This will cause ClosedChannelException:
      // val future = e.getFuture
      // Channels.write(ctx, future, region)

      val future = Channels.write(ctx.getChannel, region)
      future.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          region.releaseExternalResources
        }
      })
    }
  }
}
