package xitrum.controller

import org.jboss.netty.handler.codec.http.{HttpHeaders, HttpResponseStatus}

import xitrum.Controller

trait BasicAuth {
  this: Controller =>

  /** f takes username and password, and returns true if it want to let the user in. */
  def basicAuth(realm: String)(f: (String, String) => Boolean): Boolean =
    xitrum.handler.up.BasicAuth.basicAuth(channel, request, response, realm)(f)
}
