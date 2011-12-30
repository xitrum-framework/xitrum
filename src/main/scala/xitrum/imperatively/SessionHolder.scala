package xitrum.imperatively

import xitrum.scope.session.Session

object SessionHolder extends java.lang.ThreadLocal[Session] {
  def session: Session = get
}
