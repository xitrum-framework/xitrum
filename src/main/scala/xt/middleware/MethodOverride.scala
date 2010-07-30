package st.middleware

import scala.collection.mutable.Map
import scala.collection.JavaConversions

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod}

/**
 * This middleware puts the request method (org.jboss.netty.handler.codec.http.HttpMethod)
 * to env under the key "request_method".
 *
 * If the real request method is POST and "_method" param exists, the "_method"
 * param will override the POST method.
 *
 * This middleware should be put behind ParamsParser.
 */
object MethodOverride {
  def wrap(app: App) = new App {
    def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
      val m1 = request.getMethod
      val m2: HttpMethod = if (m1 != HttpMethod.POST)
        m1
      else {
        val params = env("params").asInstanceOf[java.util.Map[String, java.util.List[String]]]
        val _methods = params.get("_method")
        if (_methods == null || _methods.isEmpty) m1 else new HttpMethod(_methods.get(0))
      }

      env.put("request_method", m2)
      app.call(channel, request, response, env)
    }
  }
}
