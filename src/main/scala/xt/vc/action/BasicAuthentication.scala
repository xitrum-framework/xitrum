package xt.vc.action

import sun.misc.BASE64Decoder  // FIXME: http://stackoverflow.com/questions/469695/decode-base64-data-in-java
import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}
import xt.Action

trait BasicAuthentication {
  this: Action =>

  def basicAuthenticationUsernamePassword(): Option[(String, String)] = {
    val authorization = request.getHeader(HttpHeaders.Names.AUTHORIZATION)

    if (authorization == null || !authorization.startsWith("Basic ")) {
      None
    } else {
      val username_password  = authorization.substring(6)  // Skip "Basic "
      val decoder            = new BASE64Decoder
      val bytes              = decoder.decodeBuffer(username_password)
      val username_password2 = new String(bytes)
      val username_password3 = username_password2.split(":")

      if (username_password3.length != 2) {
        None
      } else {
        Some((username_password3(0), username_password3(1)))
      }
    }
  }

  def basicAuthenticationRespond(realm: String) {
    response.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"" + realm + "\"")
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)
    respond
  }

  def basicAuthenticationCheck(realm: String, username: String, password: String): Boolean = {
    basicAuthenticationUsernamePassword() match {
      case None =>
        basicAuthenticationRespond(realm)
        false

      case Some((username2, password2)) =>
        if (username2 == username && password2 == password) {
          true
        } else {
          basicAuthenticationRespond(realm)
          false
        }
    }
  }
}
