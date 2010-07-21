package xt.middleware

import scala.collection.mutable.Map
import scala.collection.JavaConversions

import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, QueryStringDecoder, HttpMethod}

/**
 * This middleware:
 *   parses params in URI and request body and puts them to env under the key "params"
 *   puts the request path to env under the key "path"
 */
object Params {
  def wrap(app: App) = new App {
    def call(req: HttpRequest, res: HttpResponse, env: Map[String, Any]) {
      val u1 = req.getUri
      val u2 = if (req.getMethod == HttpMethod.POST) {
        val c = req.getContent
        if (u1.endsWith("&")) u1 + c else u1 + "&" + c
      } else
        u1

      val d = new QueryStringDecoder(u2)
      val p = d.getParameters

      // Because we will likely put things to params in later middlewares, we need
      // to avoid UnsupportedOperationException when p is empty. Whe p is empty,
      // it is a java.util.Collections$EmptyMap, which is immutable.
      // See the source code of QueryStringDecoder as of Netty 3.2.1.Final
      val p2 = if (p.isEmpty) new java.util.LinkedHashMap[String, List[String]]() else p

      env.put("params", p2)
      env.put("path",   d.getPath)
      app.call(req, res, env)
    }
  }
}
