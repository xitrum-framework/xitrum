package xt.http_handler

import xt._

import java.io.File
import java.io.RandomAccessFile

import org.jboss.netty.channel.{ChannelUpstreamHandler, ChannelHandlerContext, ChannelEvent, MessageEvent, Channel, ChannelFuture, DefaultFileRegion, ChannelFutureListener}
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

class SendfileHandler extends ResponseHandler {
  import SendfileHandler._

  def handleResponse(ctx: ChannelHandlerContext, e: MessageEvent, response: HttpResponse) {
    if (!response.containsHeader("X-Sendfile")) {
      ctx.sendDownstream(e)
      return
    }

    val abs = response.getHeader("X-Sendfile")
    response.removeHeader("X-Sendfile")

    val channel = ctx.getChannel

    val file = new File(abs)
    if (!file.exists() || !file.isFile()) {
      response.setStatus(NOT_FOUND)
      channel.write(response)
      return
    }

    // Try to serve from cache
    val cache = CacheManager.getInstance.getCache(Config.filesEhcacheName)
    val elemKey = abs
    val elem = cache.get(elemKey)
    if (elem != null) {
      logger.debug("Serve " + abs + " from cache")
      val bytes = elem.getObjectValue.asInstanceOf[Array[Byte]]
      response.setContent(ChannelBuffers.wrappedBuffer(bytes))
      channel.write(response)
      return
    }

    var raf: RandomAccessFile = null
    try {
      raf = new RandomAccessFile(file, "r")
    } catch {
      case _ =>
        response.setStatus(NOT_FOUND)
        channel.write(response)
        return
    }

    // Cache if the file is small
    val fileLength = raf.length
    if (fileLength <= Config.filesMaxSize) {
      val bytes = new Array[Byte](fileLength.toInt)
      raf.read(bytes)
      raf.close
      cache.put(new Element(elemKey, bytes))
      response.setContent(ChannelBuffers.wrappedBuffer(bytes))
      channel.write(response)
      return
    }

    // OK, for big files we do not cache

    // Write the initial line and the header
    HttpHeaders.setContentLength(response, fileLength)
    channel.write(response)

    // Write the content
    if (channel.getPipeline.get(classOf[SslHandler]) != null) {
      // Cannot use zero-copy with HTTPS
      channel.write(new ChunkedFile(raf, 0, fileLength, CHUNK_SIZE))
    } else {
      // No encryption - use zero-copy
      val region = new DefaultFileRegion(raf.getChannel, 0, fileLength)
      val future = channel.write(region)
      future.addListener(new ChannelFutureListener {
        def operationComplete(future: ChannelFuture) {
          region.releaseExternalResources
        }
      })
    }
  }
}
