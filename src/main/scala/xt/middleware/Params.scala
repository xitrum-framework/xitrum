package xt.middleware

import scala.collection.mutable.Map
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

      env.put("params", p)
      env.put("path",   d.getPath)
      app.call(req, res, env)
    }
  }
}
