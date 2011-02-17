package xitrum.action.view

import xitrum.action.Action

object Flash {
  val FLASH_KEY           = "_flash"
  val FLASH_SAME_RESPONSE = "_flash_same_response"
}

trait Flash {
  this: Action =>

  import Flash._

  /** @see jsFlash(msg). */
  def flash(msg: Any) {
    session(FLASH_KEY)           = msg
    session(FLASH_SAME_RESPONSE) = true
  }

  /** Same as jsFlash, but for web 1.0. */
  def flash = {
    sessiono(FLASH_KEY) match {
      case None      => ""
      case Some(msg) => msg
    }
  }

  //----------------------------------------------------------------------------

  /**
   * For web 2.0 style application.
   * Used in postback action to send a message to flash area right away.
   */
  def jsFlash(msg: Any) {
    val js = jsCall("xitrum.flash", "\"" + jsEscape(msg) + "\"")
    jsRender(js)
  }

  /**
   * For web 2.0 style application.
   * Used in application layout to display the flash  message right after a view is loaded.
   */
  def jsFlash {
    val msg = flash
    if (!msg.isEmpty) {
      val js = jsCall("xitrum.flash", "\"" + jsEscape(msg) + "\"")
      jsAddToView(js)
    }
  }

  //----------------------------------------------------------------------------

  /** Called in Action's respond method. */
  def clearFlashWhenRespond {
    if (session.contains(FLASH_SAME_RESPONSE)) {
      session.delete(FLASH_SAME_RESPONSE)
    } else {
      session.delete(FLASH_KEY)
    }
  }
}
