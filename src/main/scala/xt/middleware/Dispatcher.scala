package xt.middleware

import java.lang.reflect.Method
import scala.collection.mutable.Map
import scala.collection.immutable.{Map => IMap}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpMethod, HttpResponseStatus}

import xt.framework.Controller

/**
 * This middleware should be put behind ParamsParser and MultipartParamsParser.
 */
object Dispatcher {
  type Route           = (HttpMethod, String, String)
  type CompiledPattern = Array[(String, Boolean)]  // String: token, Boolean: true if the token is constant
  type Csas            = (String, String)
  type KA              = (Class[Controller], Method)
  type CompiledCsas    = (Csas, KA)
  type CompiledRoute   = (HttpMethod, CompiledPattern, Csas, KA)
  type UriParams       = java.util.LinkedHashMap[String, java.util.List[String]]

  private var compiledRoutes: Iterable[CompiledRoute] = _

  def wrap(app: App, routes: List[Route], errorRoutes: IMap[String, String], controllerPaths: List[String]) = {
    compiledRoutes = routes.map(compileRoute(_, controllerPaths))

    val (csast404, ka404) = compileCsas(errorRoutes("404"), controllerPaths)
    val compiledCsas500   = compileCsas(errorRoutes("500"), controllerPaths)

    new App {
      def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any]) {
        val method   = env("request_method").asInstanceOf[HttpMethod]
        val pathInfo = env("path_info").asInstanceOf[String]
        val (ka, uriParams) = matchRoute(method, pathInfo) match {
          case Some((ka, uriParams)) =>
            (ka, uriParams)

          case None =>
            response.setStatus(HttpResponseStatus.NOT_FOUND)
            val uriParams = new java.util.LinkedHashMap[String, java.util.List[String]]()
            uriParams.put("controller", toValues(csast404._1))
            uriParams.put("action",     toValues(csast404._2))
            (ka404, uriParams)
        }

        // Put (csast500, ka500) to env so that they can be taken out later
        // by Failsafe midddleware
        env.put("error500", compiledCsas500)

        dispatch(app, channel, request, response, env, ka, uriParams)
      }
    }
  }

  /**
   * Also used by Failsafe when redispatching.
   */
  def dispatch(app: App,
      channel: Channel, request: HttpRequest, response: HttpResponse, env: Map[String, Any],
      ka: KA, uriParams: UriParams) {
    // Merge uriParams to params
    val params = env("params").asInstanceOf[UriParams]
    params.putAll(uriParams)

    // Put controller (Controller) and action (Method) to env so that
    // the action can be invoked at XTApp
    val (k, a) = ka
    val c = k.newInstance
    env.put("controller", c)
    env.put("action",     a)

    app.call(channel, request, response, env)
  }

  /**
   * Wraps a single String by a List.
   * Also used by Failsafe when redispatching.
   */
  def toValues(value: String): java.util.List[String] = {
    val values = new java.util.ArrayList[String](1)
    values.add(value)
    values
  }

  //----------------------------------------------------------------------------

  private def compileRoute(route: Route, controllerPaths: List[String]): CompiledRoute = {
    val (method, pattern, csas) = route
    val cp = compilePattern(pattern)
    val (csast, ka) = compileCsas(csas, controllerPaths)
    (method, cp, csast, ka)
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
   * Given "Articles#index", rerturns:
   * ("Articles", "index", Articles class, index method)
   */
  private def compileCsas(csas: String, controllerPaths: List[String]): CompiledCsas = {
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

    val a  = k.getMethod(as)

    ((cs, as), (k, a))
  }

  /**
   * Returns None if not matched.
   */
  private def matchRoute(method: HttpMethod, pathInfo: String): Option[(KA, UriParams)] = {
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

println(pathInfo, "-----------------")
        if (compiledPattern.size == 0 && tokens.size == 0)
{println(0)
          true
}        else
println(1)
println(compiledPattern.map(println(_)))
          compiledPattern.forall { tc =>
println(2)
            val (token, constant) = tc
println(token, constant)
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
        false
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
}
