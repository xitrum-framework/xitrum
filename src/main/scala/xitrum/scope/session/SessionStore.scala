package xitrum.scope.session

import xitrum.scope.request.RequestEnv

trait SessionStore {
  def start()
  def stop()

  /**
   * Called before the response is sent to the client, but only if "restore" has
   * been called. If called, "store" is always called after "restore".
   *
   * If session is empty:
   * - If browser did not send session cookie: do nothing, do not send back useless cookie
   * - If browser did send session cookie: set max age to 0 to make browser delete session cookie immediately
   */
  def store(session: Session, env: SessionEnv)

  /**
   * Called only at the first access to the session. If session is not used,
   * no proccessing is performed. If called, "restore" is always called before "store".
   */
  def restore(env: SessionEnv): Session
}
