package xitrum.routing

import java.lang.reflect.Method
import com.esotericsoftware.reflectasm.ConstructorAccess

import xitrum.Controller
import xitrum.controller.Action

object ControllerReflection {
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

  def newControllerAndAction(actionMethod: Method): (Controller, Action) = {
    val controllerClass = actionMethod.getDeclaringClass

    // Use normal Java reflection
    //val controller = controllerClass.newInstance().asInstanceOf[Controller]

    // Use ReflectASM, which is included by Twitter Chill
    // https://code.google.com/p/reflectasm/
    val access     = ConstructorAccess.get(controllerClass)
    val controller = access.newInstance().asInstanceOf[Controller]

    val newAction        = actionMethod.invoke(controller).asInstanceOf[Action]
    val withActionMethod = Action(newAction.route, actionMethod, newAction.body, newAction.cacheSeconds)
    (controller, withActionMethod)
  }
}
