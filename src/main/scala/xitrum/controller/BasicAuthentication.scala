package xitrum.controller

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}

import xitrum.Controller
import xitrum.util.Base64
import xitrum.handler.up.GlobalBasicAuthentication

trait BasicAuthentication {
  this: Controller =>

  /** f takes username and password, and returns true if it want to let the user in. */
  def basicAuthenticate(realm: String)(f: (String, String) => Boolean): Boolean =
    GlobalBasicAuthentication.basicAuthenticate(channel, request, response, realm)(f)
}
