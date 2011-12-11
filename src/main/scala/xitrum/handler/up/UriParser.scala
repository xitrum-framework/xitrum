package xitrum.handler.up

import java.util.{Map => JMap, List => JList, LinkedHashMap => JLinkedHashMap}
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import io.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, Channels}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.QueryStringDecoder

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.scope.request.{Params, PathInfo}

@Sharable
class UriParser extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[HandlerEnv]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[HandlerEnv]
    val request = env.request

    try {
      val decoder   = new QueryStringDecoder(request.getUri, Config.requestCharset)
      env.pathInfo  = new PathInfo(decoder.getPath)
      env.uriParams = jParamsToParams(decoder.getParameters)
    } catch {
      case t =>
        val msg = "Could not parse URI: " + request.getUri
        logger.warn(msg, t)

        ctx.getChannel.close
        return
    }

    Channels.fireMessageReceived(ctx, env)
  }

  //----------------------------------------------------------------------------

  private def jParamsToParams(params: JMap[String, JList[String]]): Params = {
    val keySet = params.keySet

    val it  = keySet.iterator
    val ret = MMap[String, List[String]]()
    while (it.hasNext) {
      val key    = it.next
      val values = params.get(key)

      val it2  = values.iterator
      val ret2 = ArrayBuffer[String]()
      while (it2.hasNext) {
        val value = it2.next
        ret2.append(value)
      }
      ret(key) = ret2.toList
    }

    ret
  }
}
