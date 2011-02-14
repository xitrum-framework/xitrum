package xitrum.vc.action

import xitrum.Action

object Flash {
  val FLASH_KEY           = "_flash"
  val FLASH_SAME_RESPONSE = "_flash_same_response"
}

trait Flash {
  this: Action =>

  import Flash._

  def flash(msg: Any) {
    session(FLASH_KEY)           = msg
    session(FLASH_SAME_RESPONSE) = true
  }

  def flash = session(FLASH_KEY)

  def clearFlashWhenRespond {
    if (session(FLASH_SAME_RESPONSE).isDefined) {
      session.delete(FLASH_SAME_RESPONSE)
    } else {
      session.delete(FLASH_KEY)
    }
  }
}
