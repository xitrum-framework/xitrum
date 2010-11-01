package xt.middleware

import java.lang.reflect.Method
import scala.collection.mutable.{Map, HashMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpResponseStatus}
import org.jboss.netty.buffer.ChannelBuffers
import org.jboss.netty.util.CharsetUtil

import xt._

/**
 * Runs the app with exception handler.
 * This middleware should be put behind Dispatcher and should be the last in
 * the middleware chain.
 */
object Failsafe extends Logger {
  class MissingParam(key: String) extends Throwable(key)

  def wrap(app: App) = new App {
    def call(remoteIp: String, channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
      def processThrowable(t: Throwable) {
        try {
          logger.error("xt.middleware.Failsafe", t)

          // Replace the intended controller/action with those of error 500
          // and run the app again
          val (csast, ka) = env("error500").asInstanceOf[Dispatcher.CompiledCsas]
          val (cs, as)    = csast

          val uriParams = new java.util.LinkedHashMap[String, java.util.List[String]]()
          uriParams.put("controller", Dispatcher.toValues(cs))
          uriParams.put("action",     Dispatcher.toValues(as))

          response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
          Dispatcher.dispatch(app, remoteIp, channel, request, response, env, ka, uriParams)
        } catch {
          case t2 =>
            logger.error("xt.middleware.Failsafe", t2)
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR)
            response.setContent(ChannelBuffers.copiedBuffer("Internal Server Error", CharsetUtil.UTF_8))
        }
      }

      try {
        app.call(remoteIp, channel, request, response, env)
      } catch {
        case ite: java.lang.reflect.InvocationTargetException =>
          val te = ite.getTargetException
          if (te.isInstanceOf[MissingParam]) {
            val mp = te.asInstanceOf[MissingParam]
            val msg = "Missing Param: " + mp.getMessage
            logger.debug(msg, mp)
            response.setStatus(HttpResponseStatus.BAD_REQUEST)
            response.setContent(ChannelBuffers.copiedBuffer(msg, CharsetUtil.UTF_8))
          } else {
            processThrowable(te)
          }

        case t =>
          processThrowable(t)
      }
    }
  }
}
