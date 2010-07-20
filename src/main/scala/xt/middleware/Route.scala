package xt.middleware

import java.lang.reflect.Method
import scala.collection.mutable.Map
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod}

/**
 * This middleware should be put behind Params and MultipartParams.
 */
object Route {
  def wrap(app: App, routes: List[(HttpMethod, String, String)]) = {
    val croutes = routes.map(compileRoute(_))

    new App {
      def call(req: HttpRequest, res: HttpResponse, env: Map[String, Any]) {
        env.put("controller", "Articles")
        env.put("action",     "index")
        app.call(req, res, env)
      }
    }
  }

  private def compileRoute(route: (HttpMethod, String, String)): (HttpMethod, String, (Any, Method)) = {
    val (hMethod, pattern, csas) = route
    val caa = csas.split("#")
    val cs = caa(0)
    val as = caa(1)
    val c = Class.forName(cs)
    val i = c.newInstance
    val m = c.getMethod(cs)
    (hMethod, pattern, (i, m))
  }
}
