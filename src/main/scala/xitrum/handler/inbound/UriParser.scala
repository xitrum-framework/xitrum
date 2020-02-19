package xitrum.handler.inbound

import java.util.{Map => JMap, List => JList}
import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.util.control.NonFatal

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, SimpleChannelInboundHandler}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.QueryStringDecoder

import xitrum.{Config, Log}
import xitrum.handler.HandlerEnv
import xitrum.scope.request.{Params, PathInfo}

@Sharable
class UriParser extends SimpleChannelInboundHandler[HandlerEnv] {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv): Unit = {
    val request = env.request

    try {
      val decoder     = new QueryStringDecoder(request.uri, Config.xitrum.request.charset)
      env.pathInfo    = new PathInfo(decoder, Config.xitrum.request.charset)
      env.queryParams = jParamsToParams(decoder.parameters)
      ctx.fireChannelRead(env)
    } catch {
      case NonFatal(e) =>
        Log.debug(s"Could not parse query params URI: ${request.uri}", e)
        BadClientSilencer.respond400(ctx.channel, "Could not parse params in URI")
    }
  }

  //----------------------------------------------------------------------------

  private def jParamsToParams(params: JMap[String, JList[String]]): Params = {
    val keySet = params.keySet

    val it  = keySet.iterator
    val ret = MMap.empty[String, Seq[String]]
    while (it.hasNext) {
      val key    = it.next()
      val values = params.get(key)

      val it2  = values.iterator
      val ret2 = ArrayBuffer.empty[String]
      while (it2.hasNext) {
        val value = it2.next()
        ret2.append(value)
      }
      ret(key) = ret2.toList
    }

    ret
  }
}
