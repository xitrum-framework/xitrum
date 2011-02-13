package xt.vc.action

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}

import xt.Action
import xt.vc.env.session.Base64

trait BasicAuthentication {
  this: Action =>

  def basicAuthenticationUsernamePassword(): Option[(String, String)] = {
    val authorization = request.getHeader(HttpHeaders.Names.AUTHORIZATION)

    if (authorization == null || !authorization.startsWith("Basic ")) {
      None
    } else {
      val username_password = authorization.substring(6)  // Skip "Basic "
      val bytes = Base64.decode(username_password)

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
