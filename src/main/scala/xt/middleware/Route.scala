package xt.middleware

import java.lang.reflect.Method
import scala.collection.mutable.Map
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod}

/**
 * This middleware should be put behind Params and MultipartParams.
 */
object Route {
  private type Route           = (HttpMethod, String, String)
  private type CompiledPattern = Array[(String, Boolean)]  // String: token, Boolean: true if the token is constant
  private type CsAs            = (String, String)
  private type CompiledCA      = (Any, Method)  // Any: an instance of controller
  private type CompiledRoute   = (HttpMethod, CompiledPattern, CsAs, CompiledCA)
  private type UriParams       = java.util.LinkedHashMap[String, java.util.List[String]]

  private var compiledRoutes: Iterable[CompiledRoute] = _

  def wrap(app: App, routes: List[Route], controllerPackages: List[String]) = {
    compiledRoutes = routes.map(compileRoute(_))

    val (controller404, action404) = findErrorCA("404")
    val (controller500, action500) = findErrorCA("500")

    new App {
      def call(req: HttpRequest, res: HttpResponse, env: Map[String, Any]) {
        val method = req.getMethod
        val path   = env("path").asInstanceOf[String]
        matchRoute(method, path) match {
          case Some((ca, uriParams)) =>

            val params = env("params").asInstanceOf[UriParams]
            params.putAll(uriParams)

            // Controller: Any, Action: Method
            val (c, a) = ca
            env.put("controller", c)
            env.put("action",     a)

          case None =>
        }

        // Controller: Any, Action: Method
        env.put("controller404", controller404)
        env.put("action404",     action404)
        env.put("controller500", controller500)
        env.put("action500",     action500)

        app.call(req, res, env)
      }
    }
  }

  /**
   * Find in compiledRoutes and returns (controller, action) corresponding to errorCode.
   *   controller: Any
   *   action    : Method
   */
  private def findErrorCA(errorCode: String) = {
    val crOption = compiledRoutes.find { cr =>
      val (method, cp, _, _) = cr
      (method == null) && (cp(0)._1 == errorCode)
    }
    val cr = crOption.get
    cr._4
  }

  private def compileRoute(route: Route): CompiledRoute = {
    val (method, pattern, csas) = route
    val caa = csas.split("#")
    val cs  = caa(0)
    val as  = caa(1)
    val k   = Class.forName(cs)
    val c   = k.newInstance
    val a   = k.getMethod(as)
    (method, compilePattern(pattern), (cs, as), (c, a))
  }

  private def compilePattern(pattern: String): CompiledPattern = {
    val tokens = pattern.split("/").filter(_ != "")
    tokens.map { e: String =>
      val constant = !e.startsWith(":")
      val token    = if (constant) e else e.substring(1)
      (token, constant)
    }
  }

  /**
   * Returns None if not matched.
   */
  private def matchRoute(method: HttpMethod, path: String): (Option[(CompiledCA, UriParams)]) = {
    val tokens = path.split("/").filter(_ != "")
    var uriParams: UriParams = null

    val finder = (cr: CompiledRoute) => {
      val (m, compiledPattern, csas, compiledCA) = cr
      if (m != method)
        false
      else {
        uriParams = new java.util.LinkedHashMap[String, java.util.List[String]]()
        var i = 0
        val m = tokens.size

        compiledPattern.forall { tc =>
          val (token, constant) = tc
          val ret = if (constant)
            (i < m && token == tokens(i))
          else {
            val value = if (token == "*") tokens.slice(i, m).mkString("/") else tokens(i)
            uriParams.put(token, toValues(value))
            true
          }

          i += 1
          ret
        }
      }
    }
    compiledRoutes.find(finder) match {
      case Some(cr) =>
        val (m, compiledPattern, csas, compiledCA) = cr
        val (cs, as) = csas
        uriParams.put("controller", toValues(cs))
        uriParams.put("action",     toValues(as))
        Some((compiledCA, uriParams))

      case None => None
    }
  }

  private def toValues(value: String): java.util.ArrayList[String] = {
    val values = new java.util.ArrayList[String](1)
    values.add(value)
    values
  }
}
