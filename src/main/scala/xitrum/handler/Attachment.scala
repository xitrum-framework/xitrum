package xitrum.handler

import org.jboss.netty.handler.codec.http.HttpRequest
import xitrum.scope.request.PathInfo

/** Shared data among all handlers in pipeline. */
case class Attachment(request: HttpRequest, pathInfo:Option[PathInfo])
