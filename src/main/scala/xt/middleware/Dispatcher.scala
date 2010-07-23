package xt.middleware

import java.lang.reflect.Method
import scala.collection.mutable.Map

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod}

import xt.framework.{Controller, ViewCache}

/**
 * This middleware should be put behind ParamsParser and MultipartParamsParser.
 */
object Dispatcher {
  private type Route           = (HttpMethod, String, String)
  private type CompiledPattern = Array[(String, Boolean)]  // String: token, Boolean: true if the token is constant
  private type CsAs            = (String, String)
  private type CompiledKA      = (Class[Controller], Method)
  private type CompiledRoute   = (HttpMethod, CompiledPattern, CsAs, CompiledKA)
  private type UriParams       = java.util.LinkedHashMap[String, java.util.List[String]]

  private var compiledRoutes: Iterable[CompiledRoute] = _

  def wrap(app: App, routes: List[Route], controllerPaths: List[String], viewPaths: List[String]) = {
    ViewCache.viewPaths = viewPaths

    compiledRoutes = routes.map(compileRoute(_, controllerPaths))

    val (controller404, action404) = findErrorCA("404")
    val (controller500, action500) = findErrorCA("500")

    new App {
      def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
        val method   = env("request_method").asInstanceOf[HttpMethod]
        val pathInfo = env("path_info").asInstanceOf[String]
        matchRoute(method, pathInfo) match {
          case Some((ka, uriParams)) =>

            val params = env("params").asInstanceOf[UriParams]
            params.putAll(uriParams)

            // Controller: Any, Action: Method
            val (k, a) = ka
            val c = k.newInstance
            env.put("controller", c)
            env.put("action",     a)

          case None =>
        }

        // Controller: Any, Action: Method
        env.put("controller404", controller404)
        env.put("action404",     action404)
        env.put("controller500", controller500)
        env.put("action500",     action500)

        app.call(channel, request, response, env)
      }
    }
  }

  /**
   * Find in compiledRoutes and returns (controller, action) corresponding to errorCode.
   *   controller: Any
   *   action    : Method
   */
  private def findErrorCA(errorCode: String): (Controller, Method) = {
    val crOption = compiledRoutes.find { cr =>
      val (method, cp, _, _) = cr
      (method == null) && (cp(0)._1 == errorCode)
    }
    val cr = crOption.get
    val (k, a) = cr._4
    val c = k.newInstance
    (c, a)
  }

  private def compileRoute(route: Route, controllerPaths: List[String]): CompiledRoute = {
    val (method, pattern, csas) = route
    val caa = csas.split("#")
    val cs  = caa(0)
    val as  = caa(1)

    var k: Class[Controller] = null
    controllerPaths.find { p =>
      try {
        k = Class.forName(p + "." + cs).asInstanceOf[Class[Controller]]
        true
      } catch {
        case _ => false
      }
    }
    if (k == null) throw(new Exception("Could not load " + csas))

    val a = k.getMethod(as)
    (method, compilePattern(pattern), (cs, as), (k, a))
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
  private def matchRoute(method: HttpMethod, pathInfo: String): (Option[(CompiledKA, UriParams)]) = {
    val tokens = pathInfo.split("/").filter(_ != "")
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
        val (m, compiledPattern, csas, compiledKA) = cr
        val (cs, as) = csas
        uriParams.put("controller", toValues(cs))
        uriParams.put("action",     toValues(as))
        Some((compiledKA, uriParams))

      case None => None
    }
  }

  /**
   * Wraps a single String by a List.
   */
  private def toValues(value: String): java.util.List[String] = {
    val values = new java.util.ArrayList[String](1)
    values.add(value)
    values
  }
}
