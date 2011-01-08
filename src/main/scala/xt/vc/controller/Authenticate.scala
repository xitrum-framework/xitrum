package xt.vc.controller

import sun.misc.BASE64Decoder  // FIXME: http://stackoverflow.com/questions/469695/decode-base64-data-in-java
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import xt.Controller

trait Authenticate {
  this: Controller =>

  def basicAuthenticate(realm: String, username: String, password: String): Boolean = {
    val authorization = request.getHeader(HttpHeaders.Names.AUTHORIZATION)
    if (authorization == null || !authorization.startsWith("Basic ")) {
      response.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"" + realm + "\"")
      response.setStatus(HttpResponseStatus.UNAUTHORIZED)
      respond
      false
    } else {
      val username_password = authorization.substring(6)  // Skip "Basic "
      val decoder = new BASE64Decoder
      val bytes = decoder.decodeBuffer(username_password)
      val username_password2 = new String(bytes)

      val username_password3 = username_password2.split(":")
      if (username_password3.length != 2) {
        false
      } else {
        username_password3(0) == username && username_password3(1) == password
      }
    }
  }
}
