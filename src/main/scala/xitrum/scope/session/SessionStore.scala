package xitrum.scope.session

import xitrum.scope.request.ExtEnv

trait SessionStore {
  /**
   * Called only at the first access to the session. If session is not used,
   * no proccessing is performed.
   */
  def restore(extEnv: ExtEnv): Session

  /**
   * Called before the response is sent to the client, but only if "restore" has
   * been called.
   */
  def store(session: Session, extEnv: ExtEnv)
}
