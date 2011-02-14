package xitrum.handler.up

import java.util.{List => JList}
import java.nio.charset.Charset

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpMethod, QueryStringDecoder}
import HttpMethod._

import xitrum.Config
import xitrum.handler.Env
import xitrum.vc.env.{Env => CEnv, PathInfo}

@Sharable
class BodyParser extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[Env]
    val request = env("request").asInstanceOf[HttpRequest]

    val bodyParams: CEnv.Params = if (request.getMethod != POST) {
      java.util.Collections.emptyMap[String, JList[String]]
    } else {
      val c1 = request.getContent  // ChannelBuffer
      val c2 = c1.toString(Config.paramCharset)
      val query = "?" + c2

      try {
        val decoder = new QueryStringDecoder(query, Config.paramCharset)
        decoder.getParameters
      } catch {
        case t =>
          val msg = "Could not parse POST body, URI: " + request.getUri + ", body: " + c2
          logger.warn(msg, t)

          ctx.getChannel.close
          return
      }
    }

    env("bodyParams") = bodyParams
    Channels.fireMessageReceived(ctx, env)
  }
}
