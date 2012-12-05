package xitrum.scope.request

import scala.collection.mutable.{ArrayBuffer, HashMap}

import org.jboss.netty.handler.codec.http.{HttpRequest, Cookie, CookieDecoder, CookieEncoder, HttpHeaders}
import HttpHeaders.Names

import xitrum.{Config, Controller}
import xitrum.scope.session.CSRF

trait ExtEnv extends RequestEnv with ParamAccess with CSRF {
  this: Controller =>

  // Below are lazy because they are not always accessed by framwork/application
  // (to save calculation time) or the things they depend on are null when this
  // instance is created

  lazy val at = new At

  /**
   * Browsers will not send cookie attributes back to the server. They will only
   * send the cookie’s name-value pair.
   * http://en.wikipedia.org/wiki/HTTP_cookie#Cookie_attributes
   */
  lazy val requestCookies: Map[String, String] = {
    val decoder = new CookieDecoder
    val header  = request.getHeader(Names.COOKIE)
    if (header != null) {
      Map[String, String]()
    } else {
      val cookies  = decoder.decode(header)
      val iterator = cookies.iterator
      val acc      = new HashMap[String, String]
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
    Config.sessionStore.restore(this)
  }

  def sessiono[T](key: String): Option[T] = session.get(key).map(_.asInstanceOf[T])

  def setCookieAndSessionIfTouchedOnRespond() {
    if (sessionTouched)
      // cookies is typically touched here
      Config.sessionStore.store(session, this)

    if (responseCookies.nonEmpty) {
      // Cookies sent by browser do not contain path,
      // automatically set to avoid duplicate cookies
      val basePath = Config.withBaseUrl("/")

      // http://en.wikipedia.org/wiki/HTTP_cookie
      // Server needs to SET_COOKIE multiple times
      responseCookies.foreach { cookie =>
        val encoder = new CookieEncoder(true)
        if (cookie.getPath() == null) {
          cookie.setPath(basePath)
          cookie.setHttpOnly(true)
        }
        encoder.addCookie(cookie)
        response.addHeader(Names.SET_COOKIE, encoder.encode())
      }
    }
  }
}
