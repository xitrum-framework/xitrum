package xt.handler.up

import xt.handler._

import org.jboss.netty.channel._
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod}

/**
 * This middleware puts the request method env.method.
 *
 * If the real request method is POST and "_method" param exists, the "_method"
 * param will override the POST method.
 *
 * This middleware should be put behind ParamsParser.
 */
class MethodOverride extends RequestHandler {
  def handleRequest(ctx: ChannelHandlerContext, env: XtEnv) {
    import env._

    val m1 = request.getMethod
    val m2: HttpMethod = if (m1 != HttpMethod.POST) {
      m1
    } else {
      val _methods = env.params.get("_method")
      if (_methods == null || _methods.isEmpty) m1 else new HttpMethod(_methods.get(0))
    }

    env.method = m2
    Channels.fireMessageReceived(ctx, env)
  }
}
