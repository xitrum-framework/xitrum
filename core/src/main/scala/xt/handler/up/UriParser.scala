package xt.handler.up

import xt.Logger

import java.util.{Map => JMap, List => JList}

import org.jboss.netty.channel.{SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import org.jboss.netty.handler.codec.http.{HttpRequest, QueryStringDecoder}

/**
 * @param pathInfo URL: http://example.com/articles?page=2 => pathInfo: /articles
 */
case class UriParserResult(request: HttpRequest, pathInfo: String, uriParams: JMap[String, JList[String]])

class UriParser extends SimpleChannelUpstreamHandler with Logger {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HttpRequest]) {
      ctx.sendUpstream(e)
      return
    }

    val request   = m.asInstanceOf[HttpRequest]
    val decoder   = new QueryStringDecoder(request.getUri)
    val pathInfo  = decoder.getPath
    val uriParams = decoder.getParameters
    Channels.fireMessageReceived(ctx, UriParserResult(request, pathInfo, uriParams))
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent) {
    logger.error("UriParser", e.getCause)
    e.getChannel.close
  }
}
