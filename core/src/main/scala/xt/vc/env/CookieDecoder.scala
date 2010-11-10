package xt.vc.env

import xt.vc.Env

import java.util.{UUID, HashMap => JMap, Set => JSet, TreeSet}

import org.jboss.netty.handler.codec.http.{HttpHeaders, CookieDecoder => NCookieDecoder, Cookie}
import HttpHeaders.Names._

/** Decodes once so that the application does not have to decode again and again */
object CookieDecoder {
  def decode(env: Env): JSet[Cookie] = {
    val decoder = new NCookieDecoder
    val header  = env.request.getHeader(COOKIE)
    if (header != null) decoder.decode(header) else new TreeSet[Cookie]()
  }
}
