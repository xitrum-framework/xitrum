package xitrum

import org.jboss.netty.channel.{ChannelFuture, ChannelFutureListener}

import xitrum.controller._
import xitrum.scope.request.ExtEnv
import xitrum.view.{Renderer, Responder}

trait Controller extends ExtEnv
    with ActionFactory
    with Logger
    with Net
    with Filter
    with BasicAuth
    with WebSocket
    with Redirect
    with UrlFor
    with Renderer
    with Responder
    with I18n {

  var pathPrefix = ""

  implicit val currentController: Controller = this

  // Use "lazy val" instead of "def" to prevent this action from being picked by RouteCollector
  lazy val currentAction = handlerEnv.action

  def addConnectionClosedListener(listener: => Unit) {
    channel.getCloseFuture.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) { listener }
    })
  }

  /** You can write: if (currentController == MyController) ... */
  override def equals(controller: Any): Boolean = {
    // Override "equals" instead of "==" because "equals" is only called if "this" is not null
    // http://stackoverflow.com/questions/7055299/whats-the-difference-between-null-last-and-null-eq-last-in-scala

    if (controller == null) return false

    // In this case, the class name will end with "$"
    // object MyController extends MyController
    // class MyController extends Controller
    val className1 = getClass.getName
    val className2 = controller.getClass.getName

    val withDollar1 = if (className1.endsWith("$")) className1 else className1 + "$"
    val withDollar2 = if (className2.endsWith("$")) className2 else className2 + "$"

    withDollar1 == withDollar2
  }
}
