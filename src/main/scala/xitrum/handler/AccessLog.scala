package xitrum.handler

import java.net.SocketAddress
import scala.collection.mutable.{Map => MMap}

import nl.grons.metrics4.scala.Histogram
import io.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.{Action, Config, Log}
import xitrum.scope.request.RequestEnv
import xitrum.sockjs.SockJsAction
import xitrum.action.Net

object AccessLog {
  def logFlashSocketPolicyFileAccess(remoteAddress: SocketAddress): Unit = {
    Log.info(Net.clientIp(remoteAddress) + " (flash socket policy file)")
  }

  def logStaticFileAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse): Unit = {
    Log.info(
      Net.remoteIp(remoteAddress, request) + " " +
      request.method + " " +
      request.uri + " -> " +
      response.status.code +
      " (static file)"
    )
  }

  def logResourceInJarAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse): Unit = {
    Log.info(
      Net.remoteIp(remoteAddress, request) + " " +
      request.method + " " +
      request.uri + " -> " +
      response.status.code +
      " (JAR resource)"
    )
  }

  def logActionAccess(action: Action, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null): Unit = {
    if (e == null) {
      Log.info(msgWithTime(action, beginTimestamp) + extraInfo(action, cacheSecs, hit))
    } else {
      Log.error("Dispatch error " + msgWithTime(action, beginTimestamp) + extraInfo(action, cacheSecs, hit), e)
    }
  }

  def logWebSocketAccess(action: Action, beginTimestamp: Long): Unit = {
    Log.info(msgWithTime(action, beginTimestamp) + extraInfo(action, 0, hit = false))
  }

  def logOPTIONS(request: HttpRequest): Unit = {
    Log.info("OPTIONS " + request.uri)
  }

  //----------------------------------------------------------------------------

  val LAST_EXECUTION_TIME_GAUGE = "lastExecutionTime"
  val EXECUTION_TIME_HISTOGRAM  = "executionTime"

  // Save last execution time of each access in
  // Map(actionName -> Array[timestamp, executionTime]).
  // The number of actions in a program is limited, so this won't go without bounds.
  private val lastExecutionTimeMap = MMap[String, Array[Long]]()
  xitrum.Metrics.gauge(LAST_EXECUTION_TIME_GAUGE) {
    lastExecutionTimeMap.toArray
  }

  private def msgWithTime(action: Action, beginTimestamp: Long): String = {
    val endTimestamp = System.currentTimeMillis()
    val dt           = endTimestamp - beginTimestamp
    val env          = action.handlerEnv

    takeActionExecutionTimeMetrics(action, beginTimestamp, dt)

    val b = new StringBuilder
    b.append(action.remoteIp)
    b.append(" ")
    b.append(action.request.method)
    b.append(" ")
    b.append(action.request.uri)
    b.append(" -> ")
    b.append(action.getClass.getName)
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
      b.append(action.response.status.code)
    }
    b.append(", ")
    b.append(dt)
    b.append(" [ms]")
    b.toString
  }

  private def takeActionExecutionTimeMetrics(action: Action, beginTimestamp: Long, executionTime: Long): Unit = {
    val isSockJSMetricsChannelClient =
      action.isInstanceOf[SockJsAction] &&
      action.asInstanceOf[SockJsAction].pathPrefix == "xitrum/metrics/channel"

    // Ignore the actions of metrics itself, to avoid showing them at the metrics viewer
    if (Config.xitrum.metrics.isDefined &&
      Config.xitrum.metrics.get.actions &&
      !isSockJSMetricsChannelClient)
    {
      val actionClassName = action.getClass.getName
      takeActionExecutionTimeHistogram(actionClassName,          executionTime)
      takeActionExecutionTimeHistogram(EXECUTION_TIME_HISTOGRAM, executionTime)
      lastExecutionTimeMap(actionClassName)          = Array(beginTimestamp, executionTime)
      lastExecutionTimeMap(EXECUTION_TIME_HISTOGRAM) = Array(beginTimestamp, executionTime)
    }
  }

  private def takeActionExecutionTimeHistogram(name: String, executionTime: Long): Unit = {
    val histograms = xitrum.Metrics.registry.getHistograms
    val histogram  = Option(histograms.get(name).asInstanceOf[Histogram]).getOrElse(xitrum.Metrics.histogram(name))
    histogram += executionTime
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
