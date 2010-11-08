package xt.handler.up

import xt.handler._

import java.nio.charset.Charset

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{QueryStringDecoder, HttpMethod}

/**
 * This middleware:
 * 1. Puts the request path to env.pathInfo
 * 2. Parses params in URI and request body and puts them to env.params
 */
class ParamsParser extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: XtEnv) {
    import env._

    val u1 = request.getUri
    val u2 = if (request.getMethod != HttpMethod.POST) {
      u1
    } else {
      val c1 = request.getContent  // ChannelBuffer
      val c2 = c1.toString(Charset.forName("UTF-8"))
      val p  = if (u1.indexOf("?") == -1) "?" + c2 else "&" + c2
      u1 + p
    }

    val d = new QueryStringDecoder(u2)
    val p = d.getParameters

    // Because we will likely put things to params in later middlewares, we need
    // to avoid UnsupportedOperationException when p is empty. Whe p is empty,
    // it is a java.util.Collections$EmptyMap, which is immutable.
    //
    // See the source code of QueryStringDecoder as of Netty 3.2.3.Final
    val p2 = if (p.isEmpty) new java.util.LinkedHashMap[String, java.util.List[String]]() else p

    env.pathInfo = d.getPath
    env.params = p2
    Channels.fireMessageReceived(ctx, env)
  }
}
