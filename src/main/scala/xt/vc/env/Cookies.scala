package xt.vc.env

import java.util.{TreeSet => JTreeSet}

import org.jboss.netty.handler.codec.http.{HttpRequest, Cookie, CookieDecoder, CookieEncoder, HttpHeaders}
import HttpHeaders.Names._

import xt.{Action, Config}

class Cookies(request: HttpRequest) extends JTreeSet[Cookie] {
  {
    val decoder = new CookieDecoder
    val header  = request.getHeader(COOKIE)
    val cookies = if (header != null) decoder.decode(header) else new JTreeSet[Cookie]()

    this.addAll(cookies)
  }

  def apply(key: String): Option[Cookie] = {
    val iter = this.iterator
    while (iter.hasNext) {
      val cookie = iter.next
      if (cookie.getName == key) return Some(cookie)
    }
    None
  }

  def setCookiesWhenRespond(action: Action) {
    // Session may use cookie
    if (action.isSessionTouched) Config.sessionStore.store(action.session, action)

    if (action.isCookiesTouched) {
      val encoder = new CookieEncoder(true)
      val iter = this.iterator
      while (iter.hasNext) encoder.addCookie(iter.next)
      action.response.setHeader(SET_COOKIE, encoder.encode)
    }
  }
}
