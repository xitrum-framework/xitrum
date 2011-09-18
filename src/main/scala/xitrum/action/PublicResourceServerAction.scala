package xitrum.action

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import HttpHeaders.Names._
import HttpHeaders.Values._
import HttpResponseStatus._

import xitrum.Action
import xitrum.annotation.{First, GET}
import xitrum.util.{Mime, Loader, NotModified, PathSanitizer}

@First
@GET("/resources/public/:*")
class PublicResourceServerAction extends Action {
  override def execute {
    if (!NotModified.respondIfNotModifidedSinceServerStart(this)) {
      val path = "public/" + param("*")
      loadResource(path) match {
        case None => render404Page

        case Some(bytes) =>
          // Tell the browser to cache for a long time
          // This is OK when there's a cluster of web servers behind a load balancer
          // because the URL created by urlForResource is in the form: resource?web-server-startup-timestamp
          NotModified.setMaxAgeUntilNextServerRestart(response)

          val mimeo = Mime.get(path)
          if (mimeo.isDefined) response.setHeader(CONTENT_TYPE, mimeo.get)

          renderBinary(bytes)
      }
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Read whole file to memory. It's OK because the files are normally small.
   * No one is stupid enough to store large files in resources.
   *
   * @param path Relative to one of the elements in CLASSPATH, without leading "/"
   */
  private def loadResource(path: String): Option[Array[Byte]] = {
    PathSanitizer.sanitize(path) match {
      case None => None

      case Some(path2) =>
        // http://www.javaworld.com/javaworld/javaqa/2003-08/01-qa-0808-property.html?page=2
        val stream = getClass.getClassLoader.getResourceAsStream(path2)

        if (stream == null) {
          None
        } else {
          val bytes = Loader.bytesFromInputStream(stream)
          Some(bytes)
        }
    }
  }
}
