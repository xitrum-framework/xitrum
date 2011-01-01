package xt.vc.env

import java.util.{Map => JMap, LinkedHashMap => JLinkedHashMap, List => JList, Set => JSet, TreeSet => JTreeSet}

import org.jboss.netty.handler.codec.http.{HttpRequest, Cookie, CookieDecoder, HttpHeaders}
import HttpHeaders.Names._

class Cookies(request: HttpRequest) extends JTreeSet[Cookie] {
  {
    val decoder = new CookieDecoder
    val header  = request.getHeader(COOKIE)
    val cookies = if (header != null) decoder.decode(header) else new JTreeSet[Cookie]()

    this.addAll(cookies)
  }
}
