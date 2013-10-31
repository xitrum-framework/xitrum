package xitrum.handler

import java.net.SocketAddress
import scala.collection.mutable.{Map => MMap}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.{Action, Log}
import xitrum.scope.request.RequestEnv
import xitrum.action.Net

object AccessLog extends Log {
  def logFlashSocketPolicyFileAccess(remoteAddress: SocketAddress) {
    if (log.isDebugEnabled) {
      log.debug(Net.clientIp(remoteAddress) + " (flash socket policy file)")
    }
  }

  def logStaticFileAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    if (log.isDebugEnabled) {
      log.debug(
        Net.remoteIp(remoteAddress, request) + " " +
        request.getMethod + " " +
        request.getUri + " -> " +
        response.getStatus.getCode +
        " (static file)"
      )
    }
  }

  def logResourceInJarAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    if (log.isDebugEnabled) {
      log.debug(
        Net.remoteIp(remoteAddress, request) + " " +
        request.getMethod + " " +
        request.getUri + " -> " +
        response.getStatus.getCode +
        " (JAR resource)"
      )
    }
  }

  def logActionAccess(action: Action, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    if (e == null) {
      if (log.isDebugEnabled) log.debug(msgWithTime(action.getClass.getName, action, beginTimestamp) + extraInfo(action, cacheSecs, hit))
    } else {
      log.error("Dispatch error " + msgWithTime(action.getClass.getName, action, beginTimestamp) + extraInfo(action, cacheSecs, hit), e)
    }
  }

  def logWebSocketAccess(className: String, action: Action, beginTimestamp: Long) {
    if (log.isDebugEnabled) log.debug(msgWithTime(className, action, beginTimestamp) + extraInfo(action, 0, false))
  }

  def logOPTIONS(request: HttpRequest) {
    log.debug("OPTIONS " + request.getUri)
  }

  //----------------------------------------------------------------------------

  private def msgWithTime(className: String, action: Action, beginTimestamp: Long) = {
    val endTimestamp = System.currentTimeMillis()
    val dt           = endTimestamp - beginTimestamp
    val env          = action.handlerEnv

    action.remoteIp + " " +
    action.request.getMethod + " " +
    action.request.getUri + " -> " +
    className +
    (if (env.queryParams.nonEmpty)      ", queryParams: "      + RequestEnv.inspectParamsWithFilter(env.queryParams)      else "") +
    (if (env.bodyParams.nonEmpty)       ", bodyParams: "       + RequestEnv.inspectParamsWithFilter(env.bodyParams)       else "") +
    (if (env.pathParams.nonEmpty)       ", pathParams: "       + RequestEnv.inspectParamsWithFilter(env.pathParams)       else "") +
    (if (env.fileUploadParams.nonEmpty) ", fileUploadParams: " + RequestEnv.inspectParamsWithFilter(env.fileUploadParams) else "") +
    (if (action.isDoneResponding)       " -> "                 + action.response.getStatus.getCode                        else "") +
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
