package xitrum.handler

import java.net.SocketAddress
import scala.collection.mutable.{Map => MMap}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.{Action, Logger}
import xitrum.scope.request.RequestEnv
import xitrum.action.Net

object AccessLog extends Logger {
  def logFlashSocketPolicyFileAccess(remoteAddress: SocketAddress) {
    if (logger.isDebugEnabled) {
      logger.debug(Net.clientIp(remoteAddress) + " (flash socket policy file)")
    }
  }

  def logStaticFileAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    if (logger.isDebugEnabled) {
      logger.debug(
        Net.remoteIp(remoteAddress, request) + " " +
        request.getMethod + " " +
        request.getUri + " -> " +
        response.getStatus.getCode +
        " (static file)"
      )
    }
  }

  def logResourceInJarAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    if (logger.isDebugEnabled) {
      logger.debug(
        Net.remoteIp(remoteAddress, request) + " " +
        request.getMethod + " " +
        request.getUri + " -> " +
        response.getStatus.getCode +
        " (resource in JAR)"
      )
    }
  }

  def logDynamicContentAccess(action: Action, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    if (e == null) {
      if (logger.isDebugEnabled) logger.debug(msgWithTime(action, beginTimestamp) + extraInfo(action, cacheSecs, hit))
    } else {
      logger.error("Dispatch error " + msgWithTime(action, beginTimestamp) + extraInfo(action, cacheSecs, hit), e)
    }
  }

  //----------------------------------------------------------------------------

  private def msgWithTime(action: Action, beginTimestamp: Long) = {
    val endTimestamp = System.currentTimeMillis()
    val dt           = endTimestamp - beginTimestamp
    val env          = action.handlerEnv

    action.remoteIp + " " +
    action.request.getMethod + " " +
    action.request.getUri + " -> " +
    action.getClass.getName +
    (if (env.uriParams.nonEmpty)        ", uriParams: "        + RequestEnv.inspectParamsWithFilter(env.uriParams)        else "") +
    (if (env.bodyParams.nonEmpty)       ", bodyParams: "       + RequestEnv.inspectParamsWithFilter(env.bodyParams)       else "") +
    (if (env.pathParams.nonEmpty)       ", pathParams: "       + RequestEnv.inspectParamsWithFilter(env.pathParams)       else "") +
    (if (env.fileUploadParams.nonEmpty) ", fileUploadParams: " + RequestEnv.inspectParamsWithFilter(env.fileUploadParams) else "") +
    " -> " + action.response.getStatus.getCode +
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
