package xitrum.action

import scala.xml.Elem
import org.jboss.netty.handler.codec.http._
import HttpHeaders.Names._
import HttpResponseStatus._

import xitrum.{Config, Logger}
import xitrum.action._
import xitrum.action.env.ExtEnv
import xitrum.action.routing.Routes
import xitrum.action.validation.ValidatorInjector
import xitrum.action.view.Renderer
import xitrum.handler.up.Dispatcher

trait Action extends ExtEnv with Logger with Net with Filter with BasicAuthentication with Renderer {
  implicit def elemToValidatorInjector(elem: Elem) = new ValidatorInjector(this, elem);

  def execute {}
  def postback {}

  //----------------------------------------------------------------------------

  private var _responded: Boolean = _
  {
    _responded = false
  }

  def respond = synchronized {
    if (_responded) {
      // Print the stack trace so that application developers know where to fix
      try {
        throw new Exception
      } catch {
        case e => logger.warn("Double respond", e)
      }
    } else {
      _responded = true

      prepareWhenRespond

      henv.response = response
      ctx.getChannel.write(henv)
    }
  }

  // Called by Dispatcher
  def responded = _responded

  //----------------------------------------------------------------------------

  def urlFor[T: Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].erasure.asInstanceOf[Class[Action]]
    Routes.urlFor(this, actionClass, params:_*)
  }

  /**
   * When there are no params, the application developer can write
   * urlFor[MyAction], instead of urlFor[MyAction]().
   */
  def urlFor[T: Manifest]: String = urlFor[T]()

  def urlForThis = Routes.urlFor(this, this.getClass.asInstanceOf[Class[Action]])

  //----------------------------------------------------------------------------

  def redirectTo(location: String, status: HttpResponseStatus = FOUND) {
    response.setStatus(status)

    HttpHeaders.setContentLength(response, 0)
    response.setHeader(LOCATION, location)
    respond
  }

  def redirectTo[T: Manifest] { redirectTo(urlFor[T]) }

  def redirectTo[T: Manifest](params: (String, Any)*) { redirectTo(urlFor[T](params:_*)) }

  //----------------------------------------------------------------------------

  var isPost2: Boolean = _  // Set to true by POST2Action
  {
    isPost2 = false
  }

  def forward(actionClass: Class[Action], postback: Boolean) {
    val action = actionClass.newInstance
    action(ctx, henv)
    action.isPost2 = isPost2
    Dispatcher.dispatchWithFailsafe(action, postback)
  }
}
