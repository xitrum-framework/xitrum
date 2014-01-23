package xitrum.imperatively

import java.lang.ThreadLocal
import xitrum.scope.session.Session

object SessionHolder extends ThreadLocal[Session] {
  def session: Session = get
}
