package xitrum.view

import java.io.File
import scala.xml.{Node, NodeSeq, Xhtml}

import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.handler.codec.http.{DefaultHttpChunk, HttpChunk, HttpHeaders}
import HttpHeaders.Names.{CONTENT_TYPE, CONTENT_LENGTH, TRANSFER_ENCODING}
import HttpHeaders.Values.CHUNKED

import com.codahale.jerkson.Json

import xitrum.{Action, Config}
import xitrum.handler.updown.{XSendFile, XSendResource}

trait Renderer extends JS with Flash with I18n {
  this: Action =>

  private def writeHeaderOnFirstChunk {
    if (!isResponded) {
      response.removeHeader(CONTENT_LENGTH)
      response.setHeader(TRANSFER_ENCODING, CHUNKED)
      respond
    }
  }

  /**
   * To respond chunks (http://en.wikipedia.org/wiki/Chunked_transfer_encoding):
   * 1. Call ``response.setChunked(true)``
   * 2. Call renderXXX as many times as you want
   * 3. Lastly, call ``renderLastChunk``
   *
   * Headers are only sent on the first renderXXX call.
   */
  def renderLastChunk {
    channel.write(HttpChunk.LAST_CHUNK)
  }

  //----------------------------------------------------------------------------

  /**
   * If contentType param is not given and Content-Type header is not set, it is
   * set to "application/xml" if text param is Node or NodeSeq, otherwise it is
   * set to "text/plain".
   */
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
      } else {
        text.toString
      }

    if (!isResponded) {
      // Set content type automatically
      if (contentType != null)
        response.setHeader(CONTENT_TYPE, contentType)
      else if (!response.containsHeader(CONTENT_TYPE)) {
        if (textIsXml)
          response.setHeader(CONTENT_TYPE, "application/xml; charset=" + Config.config.request.charset)
        else
          response.setHeader(CONTENT_TYPE, "text/plain; charset=" + Config.config.request.charset)
      }
    }

    val cb = ChannelBuffers.copiedBuffer(ret, Config.requestCharset)
    if (response.isChunked) {
      writeHeaderOnFirstChunk
      val chunk = new DefaultHttpChunk(cb)
      channel.write(chunk)
    } else {
      // Content length is number of bytes, not characters!
      HttpHeaders.setContentLength(response, cb.readableBytes)
      response.setContent(cb)
      respond
    }

    ret
  }

  //----------------------------------------------------------------------------

  /** Content-Type header is set to "text/json" */
  def renderJson(any: Any) {
    val json = Json.generate(any)
    renderText(json, "text/json; charset=" + Config.config.request.charset)
  }

  //----------------------------------------------------------------------------

  var renderedView: Any = null

  def layout = renderedView

  def renderView(view: Any) {
    renderView(view, layout _)
  }

  /** Content-Type header is set to "text/html" */
  def renderView(view: Any, customLayout: () => Any) {
    renderedView = view
    val renderedLayout = customLayout.apply
    if (renderedLayout == null)
      renderText(renderedView, "text/html; charset=" + Config.config.request.charset)
    else
      renderText(renderedLayout, "text/html; charset=" + Config.config.request.charset)
  }

  //----------------------------------------------------------------------------

  /** If Content-Type header is not set, it is set to "application/octet-stream" */
  def renderBinary(bytes: Array[Byte]) {
    if (!response.containsHeader(CONTENT_TYPE))
      response.setHeader(CONTENT_TYPE, "application/octet-stream")

    val cb = ChannelBuffers.wrappedBuffer(bytes)
    if (response.isChunked) {
      writeHeaderOnFirstChunk
      val chunk = new DefaultHttpChunk(cb)
      channel.write(chunk)
    } else {
      HttpHeaders.setContentLength(response, bytes.length)
      response.setContent(cb)
      respond
    }
  }

  /**
   * Sends a file using X-SendFile.
   * If Content-Type header is not set, it is guessed from the file name.
   *
   * @param path absolute or relative to the current working directory
   *
   * In some cases, the current working directory is not always the root directory
   * of the project (https://github.com/ngocdaothanh/xitrum/issues/47), you may
   * need to use xitrum.Config.root to calculate the correct absolute path from
   * a relative path.
   *
   * To sanitize the path, use xitrum.util.PathSanitizer.
   */
  def renderFile(path: String) {
    XSendFile.setHeader(response, path)
    respond
  }

  /**
   * Sends a file from public directory in one of the entry (may be a JAR file)
   * in classpath.
   * If Content-Type header is not set, it is guessed from the file name.
   *
   * @param path Relative to an entry in classpath, without leading "/"
   */
  def renderResource(path: String) {
    XSendResource.setHeader(response, path)
    respond
  }

  //----------------------------------------------------------------------------

  def renderDefault404Page {
    XSendFile.set404Page(response)
    respond
  }

  def renderDefault500Page {
    XSendFile.set500Page(response)
    respond
  }
}
