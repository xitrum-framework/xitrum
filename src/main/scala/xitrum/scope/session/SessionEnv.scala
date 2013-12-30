package xitrum.scope.session

import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap}

import io.netty.handler.codec.http.{HttpRequest, Cookie, CookieDecoder, ServerCookieEncoder, HttpHeaders}
import HttpHeaders.Names

import xitrum.{Config, Action}

trait SessionEnv extends Csrf {
  this: Action =>

  // Below are lazy because they are not always accessed by framwork/application
  // (to save calculation time) or the things they depend on are null when this
  // instance is created

  /**
   * Browsers will not send cookie attributes back to the server. They will only
   * send the cookie (name-value pairs).
   * http://en.wikipedia.org/wiki/HTTP_cookie#Cookie_attributes
   */
  lazy val requestCookies: Map[String, String] = {
    val header  = HttpHeaders.getHeader(request, Names.COOKIE)
    if (header == null) {
      Map.empty[String, String]
    } else {
      val cookies  = CookieDecoder.decode(header)
      val iterator = cookies.iterator
      val acc      = new MHashMap[String, String]
      while (iterator.hasNext()) {
        val cookie = iterator.next()
        acc(cookie.getName) = cookie.getValue
      }
      acc.toMap
    }
  }

  val responseCookies = new ArrayBuffer[Cookie]

  // Avoid encoding, decoding cookies when session is not touched by the application
  private var sessionTouched = false

  /** To reset session: session.clear() */
  lazy val session = {
    sessionTouched = true
    Config.xitrum.session.store.restore(this)
  }

  def sessiono[T](key: String)(implicit d: T DefaultsTo String): Option[T] =
    session.get(key).map(_.asInstanceOf[T])

  def setCookieAndSessionIfTouchedOnRespond() {
    if (sessionTouched) Config.xitrum.session.store.store(session, this)

    if (responseCookies.nonEmpty) {
      // To avoid accidental duplicate cookies, if cookie path is not set,
      // set it to the site's root path
      // http://groups.google.com/group/xitrum-framework/browse_thread/thread/dbb7a8e638120b09
      val rootPath = Config.withBaseUrl("/")

      // http://en.wikipedia.org/wiki/HTTP_cookie
      // Server needs to SET_COOKIE multiple times
      responseCookies.foreach { cookie =>
        if (cookie.getPath == null) cookie.setPath(rootPath)
        HttpHeaders.addHeader(response, Names.SET_COOKIE, ServerCookieEncoder.encode(cookie))
      }
    }
  }
}
