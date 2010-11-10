package xt.handler.up

import java.util.{Map => JMap, List => JList, LinkedHashMap}
import java.nio.charset.Charset

import org.jboss.netty.channel.{SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpMethod, QueryStringDecoder}
import HttpMethod._

case class BodyParserResult(
  request:    HttpRequest,
  pathInfo:   String,
  uriParams:  JMap[String, JList[String]],
  bodyParams: JMap[String, JList[String]])

class BodyParser extends SimpleChannelUpstreamHandler {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[UriParserResult]) {
      ctx.sendUpstream(e)
      return
    }

    val upr     = m.asInstanceOf[UriParserResult]
    val request = upr.request

    val bodyParams: JMap[String, JList[String]] = if (request.getMethod != POST) {
      java.util.Collections.emptyMap[String, JList[String]]()
    } else {
      val c1 = request.getContent  // ChannelBuffer
      val c2 = c1.toString(Charset.forName("UTF-8"))
      val query = "?" + c2
      val decoder = new QueryStringDecoder(query)
      decoder.getParameters
    }

    Channels.fireMessageReceived(ctx, BodyParserResult(upr.request, upr.pathInfo, upr.uriParams, bodyParams))
  }
}
