package xitrum.handler.up

import java.util.{Map => JMap, List => JList, LinkedHashMap => JLinkedHashMap}
import scala.collection.mutable.{HashMap => MHashMap, MutableList}

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.QueryStringDecoder

import xitrum.Config
import xitrum.handler.Env
import xitrum.action.env.PathInfo

@Sharable
class UriParser extends SimpleChannelUpstreamHandler with ClosedClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[Env]
    val request = env.request

    try {
      val decoder   = new QueryStringDecoder(request.getUri, Config.paramCharset)
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

  private def jParamsToParams(params: JMap[String, JList[String]]): MHashMap[String, List[String]] = {
    val keySet = params.keySet

    val it  = keySet.iterator
    val ret = new MHashMap[String, List[String]]
    while (it.hasNext) {
      val key    = it.next
      val value2 = params.get(key)

      val it2  = value2.iterator
      val ret2 = new MutableList[String]
      while (it2.hasNext) {
        val value = it2.next
        ret2.:+(value)
      }
      ret(key) = ret2.toList
    }

    ret
  }
}
