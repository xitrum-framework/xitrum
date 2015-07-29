package xitrum.scope.session

import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.util.control.NonFatal

import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.cookie.{Cookie, ServerCookieDecoder, ServerCookieEncoder}
import HttpHeaders.Names

import xitrum.{Action, Config, Log}
import xitrum.util.DefaultsTo

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
  lazy val requestCookies: scala.collection.Map[String, String] = {
    val header = HttpHeaders.getHeader(request, Names.COOKIE)
    if (header == null) {
      Map.empty[String, String]
    } else {
      try {
        val cookies  = ServerCookieDecoder.STRICT.decode(header)
        val iterator = cookies.iterator
        val acc      = MMap.empty[String, String]
        while (iterator.hasNext) {
          val cookie = iterator.next()
          acc(cookie.name) = cookie.value
        }
        acc
      } catch {
        // Cannot always get cookie, decrypt, deserialize, and type casting due to program changes etc.
        case NonFatal(e) =>
          Log.debug(s"requestCookies is set to empty because there's problem (${e.toString}) when decoding cookies: $header")
          Map.empty[String, String]
      }
    }
  }

  lazy val responseCookies = new ArrayBuffer[Cookie]

  // Avoid encoding, decoding cookies when session is not touched by the application
  private var sessionTouched = false

  /** To reset session: session.clear() */
  lazy val session = {
    sessionTouched = true
    try {
      Config.xitrum.session.store.restore(this)
    } catch {
      // Cannot always get cookie, decrypt, deserialize, and type casting due to program changes etc.
      case NonFatal(e) =>
        Log.debug(s"session is set to empty because there's problem when restoring", e)
        MMap.empty[String, Any]
    }
  }

  def sessiono[T](key: String)(implicit d: T DefaultsTo String): Option[T] =
    session.get(key).map(_.asInstanceOf[T])

  def setCookieAndSessionIfTouchedOnRespond() {
    if (sessionTouched || Config.xitrum.session.cookieMaxAge > 0) Config.xitrum.session.store.store(session, this)

    if (responseCookies.nonEmpty) {
      // To avoid accidental duplicate cookies, if cookie path is not set,
      // set it to the site's root path
      // http://groups.google.com/group/xitrum-framework/browse_thread/thread/dbb7a8e638120b09
      val rootPath = Config.withBaseUrl("/")

      // http://en.wikipedia.org/wiki/HTTP_cookie
      // Server needs to SET_COOKIE multiple times
      responseCookies.foreach { cookie =>
        if (cookie.path == null) cookie.setPath(rootPath)
        HttpHeaders.addHeader(response, Names.SET_COOKIE, ServerCookieEncoder.STRICT.encode(cookie))
      }
    }
  }
}
