package xt.middleware

import java.lang.reflect.Method
import scala.collection.mutable.{Map, HashMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpResponseStatus}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

/**
 * Runs the app with exception handler.
 * This middleware should be put behind Dispatcher and should be the last in
 * the middleware chain.
 */
object Failsafe {
  def wrap(app: App) = new App {
    def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
      try {
        app.call(channel, request, response, env)
      } catch {
        case e1 =>
          try {
            // TODO: log
            e1.printStackTrace

            // Replace the intended controller/action with those of error 500
            // and run the app again
            val (csast, ka) = env("error500").asInstanceOf[Dispatcher.CompiledCsas]
            val (cs, as)    = csast

            val uriParams = new java.util.LinkedHashMap[String, java.util.List[String]]()
            uriParams.put("controller", Dispatcher.toValues(cs))
            uriParams.put("action",     Dispatcher.toValues(as))

            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            Dispatcher.dispatch(app, channel, request, response, env, ka, uriParams)
          } catch {
            case e2 =>
              // TODO: log
              e2.printStackTrace

              response.setContent(ChannelBuffers.copiedBuffer("Internal Server Error", CharsetUtil.UTF_8))
          }
      }
    }
  }
}
