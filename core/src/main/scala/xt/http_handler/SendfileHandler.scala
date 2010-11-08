package xt.http_handler

import xt._

import java.io.File
import java.io.RandomAccessFile

import org.jboss.netty.channel.{Channels, ChannelHandlerContext, ChannelEvent, MessageEvent, Channel, ChannelFuture, DefaultFileRegion, ChannelFutureListener}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpHeaders, HttpResponseStatus, DefaultHttpResponse, HttpVersion}
import HttpResponseStatus._
import HttpVersion._
import org.jboss.netty.handler.ssl.SslHandler
import org.jboss.netty.handler.stream.ChunkedFile
import org.jboss.netty.buffer.ChannelBuffers

import net.sf.ehcache.{CacheManager, Element}

object SendfileHandler {
  val CHUNK_SIZE = 8192
}

/**
 * This handler send file to client using various strategies:
 * 1. If the file is big: use zero-copy for HTTP or chunking for HTTPS
 * 2. If the file is small: cache in memory and use normal response
 *
 * Cache is configureed by files_ehcache_name and files_max_size in xitrum.properties.
 */
class SendfileHandler extends ResponseHandler {
  import SendfileHandler._

  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, env: XtEnv) {
    import env._

    if (!response.containsHeader("X-Sendfile")) {
      Channels.write(ctx, e.getFuture, env)
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
      Channels.write(ctx, e.getFuture, env)
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
      Channels.write(ctx, e.getFuture, env)
      return
    }

    var raf: RandomAccessFile = null
    try {
      raf = new RandomAccessFile(file, "r")
    } catch {
      case _ =>
        response.setStatus(NOT_FOUND)
        HttpHeaders.setContentLength(response, 0)
        Channels.write(ctx, e.getFuture, env)
        return
    }

    // Cache if the file is small
    val fileLength = raf.length
    if (fileLength <= Config.filesMaxSize) {
      val bytes = new Array[Byte](fileLength.toInt)
      raf.read(bytes)
      cache.put(new Element(elemKey, bytes))
      raf.close

      HttpHeaders.setContentLength(response, fileLength)
      response.setContent(ChannelBuffers.wrappedBuffer(bytes))
      Channels.write(ctx, e.getFuture, env)
      return
    }

    // OK, for big files we do not cache

    // Write the initial line and the header
    HttpHeaders.setContentLength(response, fileLength)
    Channels.write(ctx, e.getFuture, env)

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
