package xt.handler.up

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.{HttpRequest, QueryStringDecoder}

import xt.Config
import xt.handler.Env
import xt.vc.env.{Env => CEnv}
import xt.vc.env.PathInfo

@Sharable
class UriParser extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[Env]
    val request = env("request").asInstanceOf[HttpRequest]

    try {
      val decoder      = new QueryStringDecoder(request.getUri, Config.paramCharset)
      env("pathInfo")  = new PathInfo(decoder.getPath)
      env("uriParams") = decoder.getParameters
    } catch {
      case t =>
        val msg = "Could not parse URI: " + request.getUri
        logger.warn(msg, t)

        ctx.getChannel.close
        return
    }

    Channels.fireMessageReceived(ctx, env)
  }
}
