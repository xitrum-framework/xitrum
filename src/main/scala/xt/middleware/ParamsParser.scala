package xt.middleware

import java.nio.charset.Charset

import scala.collection.mutable.Map
import scala.collection.JavaConversions

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, QueryStringDecoder, HttpMethod}

/**
 * This middleware:
 *   + parses params in URI and request body and puts them to env under the key "params"
 *   + puts the request path to env under the key "path_info"
 */
object ParamsParser {
  def wrap(app: App) = new App {
    def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
      val u1 = request.getUri
      val u2 = if (request.getMethod == HttpMethod.POST) {
        val c1 = request.getContent  // ChannelBuffer
        val c2 = c1.toString(Charset.forName("UTF-8"))
        val p  = if (u1.indexOf("?") == -1) "?" + c2 else "&" + c2
        u1 + p
      } else
        u1

      val d = new QueryStringDecoder(u2)
      val p = d.getParameters

      // Because we will likely put things to params in later middlewares, we need
      // to avoid UnsupportedOperationException when p is empty. Whe p is empty,
      // it is a java.util.Collections$EmptyMap, which is immutable.
      // See the source code of QueryStringDecoder as of Netty 3.2.1.Final
      val p2 = if (p.isEmpty) new java.util.LinkedHashMap[String, java.util.List[String]]() else p

      env.put("params", p2)
      env.put("path_info", d.getPath)
      app.call(channel, request, response, env)
    }
  }
}
