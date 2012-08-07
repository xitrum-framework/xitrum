package xitrum.handler

import java.net.SocketAddress
import scala.collection.mutable.{Map => MMap}
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

import xitrum.{Controller, Logger}
import xitrum.routing.ControllerReflection
import xitrum.scope.request.RequestEnv
import xitrum.controller.Net

object AccessLog extends Logger {
  def logStaticContentAccess(remoteAddress: SocketAddress, request: HttpRequest, response: HttpResponse) {
    if (logger.isDebugEnabled) {
      logger.debug(
        Net.remoteIp(remoteAddress, request) + " " +
        request.getMethod + " " +
        request.getUri + " -> " +
        response.getStatus.getCode +
        " (static)"
      )
    }
  }

  def logDynamicContentAccess(controller: Controller, beginTimestamp: Long, cacheSecs: Int, hit: Boolean, e: Throwable = null) {
    def msgWithTime = {
      val endTimestamp = System.currentTimeMillis()
      val dt           = endTimestamp - beginTimestamp
      val env          = controller.handlerEnv

      controller.remoteIp + " " +
      controller.request.getMethod + " " +
      controller.request.getUri + " -> " +
      ControllerReflection.controllerActionName(controller.handlerEnv.action) +
      (if (env.uriParams.nonEmpty)        ", uriParams: "        + RequestEnv.inspectParamsWithFilter(env.uriParams       .asInstanceOf[MMap[String, List[Any]]]) else "") +
      (if (env.bodyParams.nonEmpty)       ", bodyParams: "       + RequestEnv.inspectParamsWithFilter(env.bodyParams      .asInstanceOf[MMap[String, List[Any]]]) else "") +
      (if (env.pathParams.nonEmpty)       ", pathParams: "       + RequestEnv.inspectParamsWithFilter(env.pathParams      .asInstanceOf[MMap[String, List[Any]]]) else "") +
      (if (env.fileUploadParams.nonEmpty) ", fileUploadParams: " + RequestEnv.inspectParamsWithFilter(env.fileUploadParams.asInstanceOf[MMap[String, List[Any]]]) else "") +
      " -> " + controller.response.getStatus.getCode +
      ", " + dt + " [ms]"
    }

    def extraInfo = {
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

    if (e == null) {
      if (logger.isDebugEnabled) logger.debug(msgWithTime + extraInfo)
    } else {
      if (logger.isErrorEnabled) logger.error("Dispatch error " + msgWithTime + extraInfo, e)
    }
  }
}
