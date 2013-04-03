package xitrum.handler

import java.net.SocketAddress
import scala.collection.mutable.{Map => MMap}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.{Controller, Logger}
import xitrum.routing.ControllerReflection
import xitrum.scope.request.RequestEnv
import xitrum.controller.Net

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

  def logDynamicContentAccess(controller: Controller, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    if (e == null) {
      if (logger.isDebugEnabled) logger.debug(msgWithTime(controller, beginTimestamp) + extraInfo(controller, cacheSecs, hit))
    } else {
      logger.error("Dispatch error " + msgWithTime(controller, beginTimestamp) + extraInfo(controller, cacheSecs, hit), e)
    }
  }

  //----------------------------------------------------------------------------

  private def msgWithTime(controller: Controller, beginTimestamp: Long) = {
    val endTimestamp = System.currentTimeMillis()
    val dt           = endTimestamp - beginTimestamp
    val env          = controller.handlerEnv

    controller.remoteIp + " " +
    controller.request.getMethod + " " +
    controller.request.getUri + " -> " +
    ControllerReflection.controllerActionName(controller.handlerEnv.action) +
    (if (env.uriParams.nonEmpty)        ", uriParams: "        + RequestEnv.inspectParamsWithFilter(env.uriParams)        else "") +
    (if (env.bodyParams.nonEmpty)       ", bodyParams: "       + RequestEnv.inspectParamsWithFilter(env.bodyParams)       else "") +
    (if (env.pathParams.nonEmpty)       ", pathParams: "       + RequestEnv.inspectParamsWithFilter(env.pathParams)       else "") +
    (if (env.fileUploadParams.nonEmpty) ", fileUploadParams: " + RequestEnv.inspectParamsWithFilter(env.fileUploadParams) else "") +
    " -> " + controller.response.getStatus.getCode +
    ", " + dt + " [ms]"
  }

  private def extraInfo(controller: Controller, cacheSecs: Int, hit: Boolean) = {
    if (cacheSecs == 0) {
      if (controller.isResponded) "" else " (async)"
    } else {
      if (hit) {
        if (cacheSecs < 0) " (action cache hit)"  else " (page cache hit)"
      } else {
        if (cacheSecs < 0) " (action cache miss)" else " (page cache miss)"
      }
    }
  }
}
