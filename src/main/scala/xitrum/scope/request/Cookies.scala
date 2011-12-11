package xitrum.scope.request

import java.util.{TreeSet => JTreeSet}

import io.netty.handler.codec.http.{HttpRequest, Cookie, CookieDecoder, CookieEncoder, HttpHeaders}
import HttpHeaders.Names._

import xitrum.Action

class Cookies(request: HttpRequest) extends JTreeSet[Cookie] {
  {
    val decoder = new CookieDecoder
    val header  = request.getHeader(COOKIE)
    val cookies = if (header != null) decoder.decode(header) else new JTreeSet[Cookie]()
    this.addAll(cookies)
  }

  /** Used by application to lookup a cookie. */
  def get(key: String): Option[Cookie] = {
    val iter = this.iterator
    while (iter.hasNext) {
      val cookie = iter.next
      if (cookie.getName == key) return Some(cookie)
    }
    None
  }

  /** Used by Xitrum. */
  def setCookiesWhenRespond(action: Action) {
    val iter = this.iterator
    // http://en.wikipedia.org/wiki/HTTP_cookie
    // Server needs to SET_COOKIE multiple times
    while (iter.hasNext) {
      val encoder = new CookieEncoder(true)
      val cookie  = iter.next
      encoder.addCookie(cookie)
      action.response.addHeader(SET_COOKIE, encoder.encode)
    }
  }
}
