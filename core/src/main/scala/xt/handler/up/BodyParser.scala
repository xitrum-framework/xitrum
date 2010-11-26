package xt.handler.up

import xt.Logger
import xt.vc.env.PathInfo

import java.util.{Map => JMap, List => JList, LinkedHashMap}
import java.nio.charset.Charset

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpMethod, QueryStringDecoder}
import HttpMethod._

case class BodyParserResult(
  request:    HttpRequest,
  pathInfo:   PathInfo,
  uriParams:  JMap[String, JList[String]],
  bodyParams: JMap[String, JList[String]])

@Sharable
class BodyParser extends SimpleChannelUpstreamHandler with Logger {
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

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("BodyParser", e.getCause)
    e.getChannel.close
  }
}
