package xitrum.handler.up

import java.util.{Map => JMap, List => JList, LinkedHashMap => JLinkedHashMap}
import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.util.control.NonFatal

import io.netty.channel.{ChannelHandler, ChannelHandlerContext, SimpleChannelInboundHandler}
import ChannelHandler.Sharable
import io.netty.handler.codec.http.QueryStringDecoder

import xitrum.Config
import xitrum.handler.HandlerEnv
import xitrum.scope.request.{Params, PathInfo}

@Sharable
class UriParser extends SimpleChannelInboundHandler[HandlerEnv] with BadClientSilencer {
  override def channelRead0(ctx: ChannelHandlerContext, env: HandlerEnv) {
    val request = env.request

    try {
      val decoder = new QueryStringDecoder(request.getUri, Config.xitrum.request.charset)
      val path    = decoder.path

      // Treat "articles" and "articles/" the same
      val noSlashSuffix =
        if (path.endsWith("/"))
          path.substring(0, path.length - 1)
        else
          path

      env.pathInfo    = new PathInfo(noSlashSuffix)
      env.queryParams = jParamsToParams(decoder.parameters)
      ctx.fireChannelRead(env)
    } catch {
      case NonFatal(e) =>
        val msg = "Could not parse query params URI: " + request.getUri
        log.warn(msg, e)
        ctx.close()
    }
  }

  //----------------------------------------------------------------------------

  private def jParamsToParams(params: JMap[String, JList[String]]): Params = {
    val keySet = params.keySet

    val it  = keySet.iterator
    val ret = MMap.empty[String, Seq[String]]
    while (it.hasNext()) {
      val key    = it.next()
      val values = params.get(key)

      val it2  = values.iterator
      val ret2 = ArrayBuffer.empty[String]
      while (it2.hasNext()) {
        val value = it2.next()
        ret2.append(value)
      }
      ret(key) = ret2.toList
    }

    ret
  }
}
