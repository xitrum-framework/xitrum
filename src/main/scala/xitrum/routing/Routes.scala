package xitrum.routing

import java.io.File
import java.lang.reflect.{Method, Modifier}

import scala.collection.mutable.{ArrayBuffer, Map => MMap, StringBuilder}
import scala.util.control.NonFatal
import scala.util.matching.Regex

import org.apache.commons.lang3.ClassUtils
import org.jboss.netty.handler.codec.http.{HttpMethod, QueryStringEncoder}

import xitrum.{Config, Action, Logger, SockJsHandler}
import xitrum.scope.request.{Params, PathInfo}
import xitrum.sockjs.SockJsAction

object Routes extends Logger {
  private val ROUTES_CACHE = "routes.cache"

  val routes = deserializeCacheFileOrRecollectWithRetry()

  /** 404.html and 500.html are used by default */
  var error404: Class[_ <: Action] = _
  var error500: Class[_ <: Action] = _

  //----------------------------------------------------------------------------

  private def deserializeCacheFileOrRecollectWithRetry(): RouteCollection = {
    try {
      logger.info("Load file " + ROUTES_CACHE + "/collect routes and action/page cache config from controllers...")
      deserializeCacheFileOrRecollectWithoutRetry()
    } catch {
      case NonFatal(e) =>
        // Maybe ROUTES_CACHE file could not be loaded because dependencies have changed.
        // Try deleting and scanning again.
        val f = new File(ROUTES_CACHE)
        if (f.exists) {
          logger.warn("Error loading file " + ROUTES_CACHE, e)

          logger.info("Delete file " + ROUTES_CACHE + " and recollect...")
          f.delete()
          try {
            deserializeCacheFileOrRecollectWithoutRetry()
          } catch {
            case e2: Exception =>
              Config.exitOnError("Could not collect routes", e2)
              throw e2
          }
        } else {
          Config.exitOnError("Could not collect routes", e)
          throw e
        }
    }
  }

  private def deserializeCacheFileOrRecollectWithoutRetry(): RouteCollection = {
    val routeCollector = new RouteCollector
    routeCollector.deserializeCacheFileOrRecollect(ROUTES_CACHE)
  }
}
