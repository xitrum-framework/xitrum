package xitrum.view

import java.io.File
import scala.xml.{Node, NodeSeq, Xhtml}

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.{DefaultHttpChunk, HttpChunk, HttpHeaders}
import HttpHeaders.Names.{CONTENT_TYPE, CONTENT_LENGTH, TRANSFER_ENCODING}
import HttpHeaders.Values.CHUNKED

import xitrum.{Action, Config}
import xitrum.handler.updown.XSendfile

/**
 * To respond chunks (http://en.wikipedia.org/wiki/Chunked_transfer_encoding):
 * 1. Call ``response.setChunked(true)``
 * 2. Call renderXXX as many times as you want
 * 3. Lastly, call ``renderLastChunk``
 *
 * Headers are only sent on the first renderXXX call.
 */
trait Renderer extends JQuery with JSCollector with Flash with I18n {
  this: Action =>

  private def writeHeaderOnFirstChunk {
    if (!responded) {
      response.removeHeader(CONTENT_LENGTH)
      response.setHeader(TRANSFER_ENCODING, CHUNKED)
      respond
    }
  }

  def renderLastChunk {
    if (ctx.getChannel.isOpen) ctx.getChannel.write(HttpChunk.LAST_CHUNK)
  }

  def renderText(text: Any, contentType: String = null): String = {
    val textIsXml = text.isInstanceOf[Node] || text.isInstanceOf[NodeSeq]

    // <br />.toString will create <br></br> which renders as 2 <br /> on some browsers!
    // http://www.scala-lang.org/node/492
    // http://www.ne.jp/asahi/hishidama/home/tech/scala/xml.html
    val ret =
      if (textIsXml) {
        if (text.isInstanceOf[Node])
          Xhtml.toXhtml(text.asInstanceOf[Node])
        else
          Xhtml.toXhtml(text.asInstanceOf[NodeSeq])
      } else
        text.toString

    if (!ctx.getChannel.isOpen) return ret


    if (!responded) {
      // Set content type automatically
      if (contentType != null)
        response.setHeader(CONTENT_TYPE, contentType)
      else if (!response.containsHeader(CONTENT_TYPE)) {
        if (textIsXml)
          response.setHeader(CONTENT_TYPE, "application/xml")
        else
          response.setHeader(CONTENT_TYPE, "text/plain")
      }
    }

    val cb = ChannelBuffers.copiedBuffer(ret, Config.paramCharset)
    if (response.isChunked) {
      writeHeaderOnFirstChunk
      val chunk = new DefaultHttpChunk(cb)
      ctx.getChannel.write(chunk)
    } else {
      // Content length is number of bytes, not characters!
      HttpHeaders.setContentLength(response, cb.readableBytes)
      response.setContent(cb)
      respond
    }

    ret
  }

  //----------------------------------------------------------------------------

  var renderedView: Any = null

  def layout = renderedView

  def renderView(view: Any) {
    if (ctx.getChannel.isOpen) renderView(view, layout _)
  }

  def renderView(view: Any, customLayout: () => Any) {
    if (!ctx.getChannel.isOpen) return

    renderedView = view
    val renderedLayout = customLayout.apply
    if (renderedLayout == null)
      renderText(renderedView, "text/html")
    else
      renderText(renderedLayout, "text/html")
  }

  //----------------------------------------------------------------------------

  def renderBinary(bytes: Array[Byte]) {
    if (!ctx.getChannel.isOpen) return

    val cb = ChannelBuffers.wrappedBuffer(bytes)
    if (response.isChunked) {
      writeHeaderOnFirstChunk
      val chunk = new DefaultHttpChunk(cb)
      ctx.getChannel.write(chunk)
    } else {
      HttpHeaders.setContentLength(response, bytes.length)
      response.setContent(cb)
      respond
    }
  }

  /**
   * Sends a file using X-Sendfile.
   *
   * @param path Starts with "/" for absolute path, otherwise it is relative to
   * the current working directory (System.getProperty("user.dir")).
   * path is not sanitized. To sanitize, use xitrum.Sanitizer.
   */
  def renderFile(path: String) {
    if (!ctx.getChannel.isOpen) return

    val abs = if (path.startsWith("/")) path else System.getProperty("user.dir") + File.separator + path
    XSendfile.setHeader(response, abs)
    respond
  }

  //----------------------------------------------------------------------------

  def render404Page {
    if (!ctx.getChannel.isOpen) return

    XSendfile.set404Page(response)
    respond
  }
}
