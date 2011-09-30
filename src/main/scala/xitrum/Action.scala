package xitrum

import java.io.File
import scala.xml.Elem

import xitrum.action._
import xitrum.handler.up.Dispatcher
import xitrum.scope.request.ExtEnv
import xitrum.validation.{Validator, ValidatorInjector}
import xitrum.view.Renderer

trait Action extends ExtEnv with Logger with Net with Filter with BasicAuthentication with Redirect with UrlFor with Renderer with I18n {
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
      if (channel.isOpen) {
        prepareWhenRespond
        handlerEnv.response = response
        channel.write(handlerEnv)
      }
    }
  }

  //----------------------------------------------------------------------------

  // For Validate to use
  implicit val action: Action = this

  /** @return Param name that has been encrypted to include serialized validators */
  def validate(paramName: String, validators: Validator*): String = {
    /* Design decision:
    App developers would write:
      <input type="text" name={validate("username", MinLength(5), MaxLength(10)} />

    This is easier to read and simpler than:
      {<input type="text" name="username" /> +: Validate(MinLength(5), MaxLength(10))}
    and he know that the resulting name may not be "username".

    This is faster than:
      {<input type="text" name="username" /> +: MinLength(5) +: MaxLength(10)}
    */

    val secureParamName = ValidatorInjector.injectToParamName(paramName, validators:_*)
    validators.foreach { v => v.render(this, paramName, secureParamName) }
    secureParamName
  }

  //----------------------------------------------------------------------------

  def addConnectionClosedListener(listener: () => Unit) {
    val dispatcher = channel.getPipeline.get(classOf[Dispatcher])
    dispatcher.addConnectionClosedListener(listener)
  }
}
