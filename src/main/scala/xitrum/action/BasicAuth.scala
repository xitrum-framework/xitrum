package xitrum.action

import xitrum.Action

trait BasicAuth {
  this: Action =>

  /**
   * @param authenticator takes username and password, returns true if it want
   * to let the user in.
   */
  def basicAuth(realm: String)(authenticator: (String, String) => Boolean): Boolean =
    xitrum.handler.inbound.BasicAuth.basicAuth(handlerEnv, realm)(authenticator)
}
