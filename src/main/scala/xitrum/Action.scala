package xitrum

import scala.xml.Elem
import org.jboss.netty.handler.codec.http._
import HttpHeaders.Names._
import HttpResponseStatus._

import xitrum.action._
import xitrum.scope.ExtEnv
import xitrum.scope.session.CSRF
import xitrum.action.routing.{PostbackAction, Routes}
import xitrum.validation.{Validator, ValidatorInjector}
import xitrum.view.Renderer
import xitrum.handler.up.Dispatcher

trait Action extends ExtEnv with Logger with Net with Filter with BasicAuthentication with Renderer {
  def execute {}
  def postback {}

  //----------------------------------------------------------------------------

  // For Validate to use
  implicit val action: Action = this

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
    Routes.urlFor(actionClass, params:_*)
  }
  def absoluteUrlFor[T: Manifest](params: (String, Any)*) = absoluteUrlPrefix + urlFor[T](params:_*)

  /**
   * When there are no params, the application developer can write
   * urlFor[MyAction], instead of urlFor[MyAction]().
   */
  def urlFor[T: Manifest]: String = urlFor[T]()
  def absoluteUrlFor[T: Manifest]: String = absoluteUrlPrefix + urlFor[T]()

  def urlForThis         = Routes.urlFor(this.getClass.asInstanceOf[Class[Action]])
  def absoluteUrlForThis = absoluteUrlPrefix + urlForThis

  def urlForThis(params: (String, Any)*) = Routes.urlFor(this.getClass.asInstanceOf[Class[Action]], params:_*)
  def absoluteUrlForThis(params: (String, Any)*) = absoluteUrlPrefix + urlForThis(params:_*)

  //----------------------------------------------------------------------------

  protected def urlForPostbackAction(actionClass: Class[Action]): String = {
    val className       = actionClass.getName
    val secureClassName = CSRF.encrypt(this, className)
    val url = PostbackAction.POSTBACK_PREFIX + secureClassName
    if (Config.baseUri.isEmpty) url else Config.baseUri + "/" + url
  }

  def urlForPostback[T: Manifest]: String = {
    val actionClass = manifest[T].erasure.asInstanceOf[Class[Action]]
    urlForPostbackAction(actionClass)
  }

  def urlForPostbackThis = urlForPostbackAction(this.getClass.asInstanceOf[Class[Action]])

  //----------------------------------------------------------------------------

  def urlForPublic(path: String) = Config.baseUri + "/public/" + path

  def urlForResource(path: String) = Config.baseUri + "/resources/public/" + path

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

  var isPostback: Boolean = _  // Set to true by PostbackAction
  {
    isPostback = false
  }

  def forward(actionClass: Class[Action], postback: Boolean) {
    val action = actionClass.newInstance
    action(ctx, henv)
    action.isPostback = isPostback
    Dispatcher.dispatchWithFailsafe(action, postback)
  }

  //----------------------------------------------------------------------------

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
}
