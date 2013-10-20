package xitrum.handler.up

import java.util.{Map => JMap, List => JList, LinkedHashMap => JLinkedHashMap}
import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.util.control.NonFatal

import org.jboss.netty.channel.{ChannelHandler, SimpleChannelUpstreamHandler, ChannelHandlerContext, MessageEvent, Channels}
import ChannelHandler.Sharable
import org.jboss.netty.handler.codec.http.QueryStringDecoder

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
      val decoder = new QueryStringDecoder(request.getUri, Config.xitrum.request.charset)
      val path    = decoder.getPath

      // Treat "articles" and "articles/" the same
      val pathWithoutTrailingSlash =
        if (path.endsWith("/"))
          path.substring(0, path.length - 1)
        else
          path

      env.pathInfo    = new PathInfo(pathWithoutTrailingSlash)
      env.queryParams = jParamsToParams(decoder.getParameters)
      ctx.sendUpstream(e)
    } catch {
      case NonFatal(e) =>
        val msg = "Could not parse URI: " + request.getUri
        logger.warn(msg, e)
        ctx.getChannel.close()
    }
  }

  //----------------------------------------------------------------------------

  private def jParamsToParams(params: JMap[String, JList[String]]): Params = {
    val keySet = params.keySet

    val it  = keySet.iterator
    val ret = MMap[String, Seq[String]]()
    while (it.hasNext()) {
      val key    = it.next()
      val values = params.get(key)

      val it2  = values.iterator
      val ret2 = ArrayBuffer[String]()
      while (it2.hasNext()) {
        val value = it2.next()
        ret2.append(value)
      }
      ret(key) = ret2.toList
    }

    ret
  }
}
