package xitrum.action

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.Action
import xitrum.annotation.{First, GET}
import xitrum.etag.{Etag, NotModified}
import xitrum.util.{Mime, PathSanitizer}

@First
@GET("/resources/public/:*")
class PublicResourceServerAction extends Action {
  override def execute {
    val path = "public/" + param("*")
    PathSanitizer.sanitize(path) match {
      case None => render404Page

      case Some(path2) =>
        Etag.forResource(path2) match {
          case Etag.NotFound => render404Page

          case Etag.Small(bytes, etag, mimeo, gzipped) =>
            if (!Etag.respondIfEtagsMatch(this, etag)) {
              response.setHeader(ETAG, etag)
              if (mimeo.isDefined) response.setHeader(CONTENT_TYPE, mimeo.get)
              if (gzipped)         response.setHeader(CONTENT_ENCODING, "gzip")

              // Tell the browser to cache for a long time
              // This works well even when this is a cluster of web servers behind a load balancer
              // because the URL created by urlForResource is in the form: resource?etag
              NotModified.setMaxAgeAggressively(response)

              renderBinary(bytes)
            }
        }
    }
  }
}
