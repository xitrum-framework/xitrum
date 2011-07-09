package xitrum.handler.up

import java.util.{Map => JMap, List => JList, LinkedHashMap => JLinkedHashMap}
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, ExceptionEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.QueryStringDecoder

import xitrum.Config
import xitrum.handler.{BaseUri, Env}
import xitrum.scope.PathInfo

@Sharable
class UriParser extends SimpleChannelUpstreamHandler with BadClientSilencer {
  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    val m = e.getMessage
    if (!m.isInstanceOf[Env]) {
      ctx.sendUpstream(e)
      return
    }

    val env     = m.asInstanceOf[Env]
    val request = env.request

    try {
      val uri = BaseUri.remove(request.getUri).get  // None has been checked at PublicFileServer
      val decoder   = new QueryStringDecoder(uri, Config.paramCharset)
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

  private def jParamsToParams(params: JMap[String, JList[String]]): MMap[String, Array[String]] = {
    val keySet = params.keySet

    val it  = keySet.iterator
    val ret = MMap[String, Array[String]]()
    while (it.hasNext) {
      val key    = it.next
      val values = params.get(key)

      val it2  = values.iterator
      val ret2 = ArrayBuffer[String]()
      while (it2.hasNext) {
        val value = it2.next
        ret2.append(value)
      }
      ret(key) = ret2.toArray
    }

    ret
  }
}
