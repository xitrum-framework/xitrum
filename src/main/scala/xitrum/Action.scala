package xitrum

import java.io.File
import scala.xml.Elem

import xitrum.action._
import xitrum.handler.up.Dispatcher
import xitrum.scope.request.ExtEnv
import xitrum.validation.{Validator, ValidatorInjector}
import xitrum.view.Renderer

trait Action extends ExtEnv with Logger with Net with Filter with BasicAuthentication with WebSocket with Redirect with UrlFor with Renderer with I18n {
  def execute {}
  def postback {}

  //----------------------------------------------------------------------------

  private var responded = false

  def isResponded = responded

  def respond = synchronized {
    if (responded) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Double response", e)
      }
    } else {
      responded = true
      prepareWhenRespond
      channel.write(handlerEnv)
    }
  }

  //----------------------------------------------------------------------------

  // For Validators to use
  implicit val action: Action = this

  //----------------------------------------------------------------------------

  def addConnectionClosedListener(listener: () => Unit) {
    val dispatcher = channel.getPipeline.get(classOf[Dispatcher])
    dispatcher.addConnectionClosedListener(listener)
  }
}
