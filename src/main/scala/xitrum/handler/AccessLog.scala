package xitrum.handler

import java.net.SocketAddress
import scala.collection.mutable.{Map => MMap}
import scala.collection.JavaConverters._

import nl.grons.metrics.scala.Histogram
import io.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.{Action, Config, Log}
import xitrum.scope.request.RequestEnv
import xitrum.action.Net

object AccessLog extends Log {
  def logFlashSocketPolicyFileAccess(remoteAddress: SocketAddress) {
    if (log.isInfoEnabled) {
      log.info(Net.clientIp(remoteAddress) + " (flash socket policy file)")
    }
  }

  def logStaticFileAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    if (log.isInfoEnabled) {
      log.info(
        Net.remoteIp(remoteAddress, request) + " " +
        request.getMethod + " " +
        request.getUri + " -> " +
        response.getStatus.code +
        " (static file)"
      )
    }
  }

  def logResourceInJarAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    if (log.isInfoEnabled) {
      log.info(
        Net.remoteIp(remoteAddress, request) + " " +
        request.getMethod + " " +
        request.getUri + " -> " +
        response.getStatus.code +
        " (JAR resource)"
      )
    }
  }

  def logActionAccess(action: Action, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    if (e == null) {
      if (log.isInfoEnabled) log.info(msgWithTime(action.getClass.getName, action, beginTimestamp) + extraInfo(action, cacheSecs, hit))
    } else {
      log.error("Dispatch error " + msgWithTime(action.getClass.getName, action, beginTimestamp) + extraInfo(action, cacheSecs, hit), e)
    }
  }

  def logWebSocketAccess(className: String, action: Action, beginTimestamp: Long) {
    if (log.isInfoEnabled) log.info(msgWithTime(className, action, beginTimestamp) + extraInfo(action, 0, false))
  }

  def logOPTIONS(request: HttpRequest) {
    log.info("OPTIONS " + request.getUri)
  }

  //----------------------------------------------------------------------------

  // Save last executeTime of each access
  // Map('actionName': [timestamp, execTime])
  private lazy val lastExecTimeMap = MMap[String, Array[Long]]()
  private val gauge = xitrum.Metrics.gauge("lastExecutionTime") {
    lastExecTimeMap.toArray
  }

  private def msgWithTime(className: String, action: Action, beginTimestamp: Long) = {
    val endTimestamp = System.currentTimeMillis()
    val dt           = endTimestamp - beginTimestamp
    val env          = action.handlerEnv
    if (Config.xitrum.metrics.isDefined && Config.xitrum.metrics.get.actions) {
      val histograms = xitrum.Metrics.registry.getHistograms()
      val histogram =
        if (histograms.containsKey(action.getClass.getName))
          histograms.get(action.getClass.getName)
        else
          xitrum.Metrics.histogram(action.getClass.getName)
      histogram.asInstanceOf[Histogram] += dt
      lastExecTimeMap(action.getClass.getName) = Array(System.currentTimeMillis, dt)
    }

    action.remoteIp + " " +
    action.request.getMethod + " " +
    action.request.getUri + " -> " +
    className +
    (if (env.queryParams.nonEmpty)    ", queryParams: "    + RequestEnv.inspectParamsWithFilter(env.queryParams)    else "") +
    (if (env.bodyTextParams.nonEmpty) ", bodyTextParams: " + RequestEnv.inspectParamsWithFilter(env.bodyTextParams) else "") +
    (if (env.pathParams.nonEmpty)     ", pathParams: "     + RequestEnv.inspectParamsWithFilter(env.pathParams)     else "") +
    (if (env.bodyFileParams.nonEmpty) ", bodyFileParams: " + RequestEnv.inspectParamsWithFilter(env.bodyFileParams) else "") +
    (if (action.isDoneResponding)       " -> "             + action.response.getStatus.code                         else "") +
    ", " + dt + " [ms]"
  }

  private def extraInfo(action: Action, cacheSecs: Int, hit: Boolean) = {
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
