package xitrum.routing

import java.lang.reflect.Method
import scala.collection.mutable.{Map => MMap}

import io.netty.handler.codec.serialization.ClassResolvers

import xitrum.Controller
import xitrum.controller.Action

object ControllerReflection {
  private val classResolver = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  /** @return "controllerName#actionName" */
  def controllerActionName(action: Action): String =
    controllerActionName(action.method)

  /** @return "controllerName#actionName" */
  def controllerActionName(actionMethod: Method): String = {
    val controllerClassName = actionMethod.getDeclaringClass.getName
    val actionName          = actionMethod.getName
    controllerClassName + "#" + actionName
  }

  /** Given "controllerName#actionName" returns (controllerName, actionName) */
  def splitControllerActionName(controllerActionName: String): (String, String) = {
    val array = controllerActionName.split('#')
    (array(0), array(1))
  }

  /** Called by Routes and newControllerAndAction */
  def getActionMethod(className: String, methodName: String, cacheActionMethod: Boolean = true): Option[Method] = {
    val klass = classResolver.resolve(className)
    if (classOf[Controller].isAssignableFrom(klass)) {
      val actionMethod = klass.getMethod(methodName)
      if (cacheActionMethod) cacheActionMethodToActionInCompanionControllerObject(className, methodName, actionMethod)
      Some(actionMethod)
    } else {
      None
    }
  }

  def newControllerAndAction(action: Action): (Controller, Action) = {
    newControllerAndAction(action.method)
  }

  def newControllerAndAction(actionMethod: Method): (Controller, Action) = {
    val controllerClass  = actionMethod.getDeclaringClass
    val controller       = controllerClass.newInstance().asInstanceOf[Controller]
    val newAction        = actionMethod.invoke(controller).asInstanceOf[Action]
    val withActionMethod = Action(newAction.route, newAction.body, actionMethod, newAction.cacheSeconds)
    (controller, withActionMethod)
  }

  /** For postback to create controller from "controllerName#actionName" */
  def newControllerAndAction(controllerActionName: String): (Controller, Action) = {
    val (controllerName, routeName) = splitControllerActionName(controllerActionName)
    val routeo = getActionMethod(controllerName, routeName, false) // No need to cache, because it has already been cached
    newControllerAndAction(routeo.get)
  }

  /**
   * Routes in controller companion object will have null routeMethod.
   * To create new controller instances or get controller class name & route name,
   * these routes need lookup this map to get non-null routeMethods.
   */
  private def cacheActionMethodToActionInCompanionControllerObject(controllerClassName: String, actionName: String, actionMethod: Method) {
    // If the controller class has no companion object,
    // ClassNotFoundException will be thrown
    try {
      val companionClass = classResolver.resolve(controllerClassName + "$")
      val rm             = companionClass.getMethod(actionName)
      val MODULE$Field   = companionClass.getField("MODULE$")
      val MODULE$        = MODULE$Field.get(null)
      val action         = rm.invoke(MODULE$).asInstanceOf[Action]
      action.method      = actionMethod
    } catch {
      case _ =>
    }
  }
}
