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

  def flasho = sessiono(FLASH_KEY)

  def clearFlashWhenRespond {
    if (session.contains(FLASH_SAME_RESPONSE)) {
      session.delete(FLASH_SAME_RESPONSE)
    } else {
      session.delete(FLASH_KEY)
    }
  }
}
