package xitrum.handler

import java.net.SocketAddress
import scala.collection.mutable.{Map => MMap}

import nl.grons.metrics.scala.Histogram
import io.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.{Action, Config, Log}
import xitrum.scope.request.RequestEnv
import xitrum.sockjs.SockJsAction
import xitrum.action.Net

object AccessLog {
  def logFlashSocketPolicyFileAccess(remoteAddress: SocketAddress) {
    Log.info(Net.clientIp(remoteAddress) + " (flash socket policy file)")
  }

  def logStaticFileAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    Log.info(
      Net.remoteIp(remoteAddress, request) + " " +
      request.getMethod + " " +
      request.getUri + " -> " +
      response.getStatus.code +
      " (static file)"
    )
  }

  def logResourceInJarAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    Log.info(
      Net.remoteIp(remoteAddress, request) + " " +
      request.getMethod + " " +
      request.getUri + " -> " +
      response.getStatus.code +
      " (JAR resource)"
    )
  }

  def logActionAccess(action: Action, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    if (e == null) {
      Log.info(msgWithTime(action.getClass.getName, action, beginTimestamp) + extraInfo(action, cacheSecs, hit))
    } else {
      Log.error("Dispatch error " + msgWithTime(action.getClass.getName, action, beginTimestamp) + extraInfo(action, cacheSecs, hit), e)
    }
  }

  def logWebSocketAccess(className: String, action: Action, beginTimestamp: Long) {
    Log.info(msgWithTime(className, action, beginTimestamp) + extraInfo(action, 0, false))
  }

  def logOPTIONS(request: HttpRequest) {
    Log.info("OPTIONS " + request.getUri)
  }

  //----------------------------------------------------------------------------

  // Save last execution time of each access in
  // Map(actionName -> Array[timestamp, executionTime]).
  // The number of actions in a program is limited, so this won't go without bounds.
  private val lastExecTimeMap = MMap[String, Array[Long]]()
  xitrum.Metrics.gauge("lastExecutionTime") {
    lastExecTimeMap.toArray
  }

  private def msgWithTime(className: String, action: Action, beginTimestamp: Long): String = {
    val endTimestamp = System.currentTimeMillis()
    val dt           = endTimestamp - beginTimestamp
    val env          = action.handlerEnv

    takeActionExecutionTimeMetrics(action, beginTimestamp, dt)

    val b = new StringBuilder
    b.append(action.remoteIp)
    b.append(" ")
    b.append(action.request.getMethod)
    b.append(" ")
    b.append(action.request.getUri)
    b.append(" -> ")
    b.append(className)
    if (env.queryParams.nonEmpty) {
      b.append(", queryParams: ")
      b.append(RequestEnv.inspectParamsWithFilter(env.queryParams))
    }
    if (env.bodyTextParams.nonEmpty) {
      b.append(", bodyTextParams: ")
      b.append(RequestEnv.inspectParamsWithFilter(env.bodyTextParams))
    }
    if (env.pathParams.nonEmpty) {
      b.append(", pathParams: ")
      b.append(RequestEnv.inspectParamsWithFilter(env.pathParams))
    }
    if (env.bodyFileParams.nonEmpty) {
      b.append(", bodyFileParams: ")
      b.append(RequestEnv.inspectParamsWithFilter(env.bodyFileParams))
    }
    if (action.isDoneResponding) {
      b.append(" -> ")
      b.append(action.response.getStatus.code)
    }
    b.append(", ")
    b.append(dt)
    b.append(" [ms]")
    b.toString
  }

  private def takeActionExecutionTimeMetrics(action: Action, beginTimestamp: Long, executionTime: Long) {
    val isSockJSMetricsChannelClient =
      action.isInstanceOf[SockJsAction] &&
      action.asInstanceOf[SockJsAction].pathPrefix == "xitrum/metrics/channel"

    // Ignore the actions of metrics itself, to avoid showing them at the metrics viewer
    if (Config.xitrum.metrics.isDefined &&
      Config.xitrum.metrics.get.actions &&
      !isSockJSMetricsChannelClient)
    {
      val histograms      = xitrum.Metrics.registry.getHistograms
      val actionClassName = action.getClass.getName
      val histogram: Histogram =
        if (histograms.containsKey(actionClassName))
          histograms.get(actionClassName).asInstanceOf[Histogram]
        else
          xitrum.Metrics.histogram(actionClassName)
      histogram += executionTime

      lastExecTimeMap(actionClassName) = Array(beginTimestamp, executionTime)
    }
  }

  private def extraInfo(action: Action, cacheSecs: Int, hit: Boolean): String = {
    if (cacheSecs == 0) {
      if (action.isDoneResponding) "" else " (async)"
    } else {
      if (hit) {
        if (cacheSecs < 0) " (action cache hit)"  else " (page cache hit)"
      } else {
        if (cacheSecs < 0) " (action cache miss)" else " (page cache miss)"
      }
    }
  }
}
