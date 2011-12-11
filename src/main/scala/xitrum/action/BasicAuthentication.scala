package xitrum.action

import io.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}

import xitrum.Action
import xitrum.util.Base64

trait BasicAuthentication {
  this: Action =>

  /**
   * f takes username and password, and returns true if it want to let the user in.
   */
  def basicAuthenticate(realm: String)(f: (String, String) => Boolean): () => Boolean = () => {
    getUsernamePassword match {
      case None =>
        respondBasic(realm)
        false

      case Some((username2, password2)) =>
        if (f(username2, password2)) {
          true
        } else {
          respondBasic(realm)
          false
        }
    }
  }

  //----------------------------------------------------------------------------

  private def getUsernamePassword: Option[(String, String)] = {
    val authorization = request.getHeader(HttpHeaders.Names.AUTHORIZATION)

    if (authorization == null || !authorization.startsWith("Basic ")) {
      None
    } else {
      val username_password = authorization.substring(6)  // Skip "Basic "
      Base64.decode(username_password) match {
        case None => None

        case Some(bytes) =>
          val username_password2 = new String(bytes)
          val username_password3 = username_password2.split(':')

          if (username_password3.length != 2) {
            None
          } else {
            Some((username_password3(0), username_password3(1)))
          }
      }
    }
  }

  private def respondBasic(realm: String) {
    response.setHeader(HttpHeaders.Names.WWW_AUTHENTICATE, "Basic realm=\"" + realm + "\"")
    response.setStatus(HttpResponseStatus.UNAUTHORIZED)
    renderText("Wrong username or password")
  }
}
