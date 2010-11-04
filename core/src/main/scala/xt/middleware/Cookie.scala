package xt.middleware

import java.util.{UUID, HashMap => JMap, TreeSet}

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse, HttpHeaders, CookieDecoder, CookieEncoder, Cookie => NCookie}
import HttpHeaders.Names._

object Cookie {
  def wrap(app: App) = new App {
    def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Env) {
      val decoder = new CookieDecoder
      val header  = request.getHeader(COOKIE)
      val cookies = if (header != null) decoder.decode(header) else new TreeSet[NCookie]()

      env.cookies = cookies
      app.call(channel, request, response, env)

      if (cookies.size > 0) {
	      val encoder = new CookieEncoder(true)
	      val iter = env.cookies.iterator
	      while (iter.hasNext) encoder.addCookie(iter.next)
	      response.setHeader(SET_COOKIE, encoder.encode)
      }
    }
  }
}
