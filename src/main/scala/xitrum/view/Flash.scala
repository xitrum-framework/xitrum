package xitrum.view

import xitrum.Controller

object Flash {
  val FLASH_KEY = "_flash"
}

trait Flash {
  this: Controller =>

  import Flash._

  /** @see jsFlash(msg). */
  def flash(msg: Any) {
    session(FLASH_KEY) = msg
  }

  /**
   * Same as jsFlash, but for web 1.0. The flash is clear right after this method
   * is called.
   */
  def flash = {
    sessiono(FLASH_KEY) match {
      case None =>
        ""

      case Some(msg) =>
        session.remove(FLASH_KEY)
        msg
    }
  }

  //----------------------------------------------------------------------------

  private def jsFlashCall(msg: Any) = "xitrum.flash(" + jsEscape(msg) + ")"

  /**
   * For web 2.0 style application.
   * Used in Ajax request handling to send a message to the flash area right away.
   */
  def jsRenderFlash(msg: Any) {
    val js = jsFlashCall(msg)
    jsRender(js)
  }

  /**
   * For web 2.0 style application.
   * Used in application layout to display the flash message right after a view is loaded.
   */
  def jsFlash(msg: Any) {
    val js = jsFlashCall(msg)
    jsAddToView(js)
  }

  lazy val xitrumCSS = <link href={urlForResource("xitrum/xitrum.css")} type="text/css" rel="stylesheet" media="all"></link>
}
