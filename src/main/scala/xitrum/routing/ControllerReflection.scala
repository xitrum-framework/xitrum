package xitrum.routing

import java.lang.reflect.Method
import scala.collection.mutable.{Map => MMap}

import io.netty.handler.codec.serialization.ClassResolvers

import xitrum.Controller

object ControllerReflection {
  private val classResolver = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  /** @return "controllerName#routeName" */
  def controllerRouteName(route: Route): String =
    controllerRouteName(route.routeMethod)

  /** @return "controllerName#routeName" */
  def controllerRouteName(routeMethod: Method): String = {
    val controllerClassName = routeMethod.getDeclaringClass.getName
    val routeName           = routeMethod.getName
    controllerClassName + "#" + routeName
  }

  /** Given "controllerName#routeName" returns (controllerName, routeName) */
  def splitControllerRouteName(controllerRouteName: String): (String, String) = {
    val array = controllerRouteName.split('#')
    (array(0), array(1))
  }

  /** Called by Routes and newControllerAndRoute */
  def getRouteMethod(className: String, methodName: String, cacheRouteMethod: Boolean = true): Option[Method] = {
    val klass = classResolver.resolve(className)
    if (classOf[Controller].isAssignableFrom(klass)) {
      val routeMethod = klass.getMethod(methodName)
      if (cacheRouteMethod) cacheRouteMethodToRouteInCompanionControllerObject(className, methodName, routeMethod)
      Some(routeMethod)
    } else {
      None
    }
  }

  def newControllerAndRoute(route: Route): (Controller, Route) = {
    newControllerAndRoute(route.routeMethod)
  }

  def newControllerAndRoute(routeMethod: Method): (Controller, Route) = {
    val controllerClass = routeMethod.getDeclaringClass
    val controller      = controllerClass.newInstance().asInstanceOf[Controller]
    val newRoute        = routeMethod.invoke(controller).asInstanceOf[Route]
    val withRouteMethod = Route(newRoute.httpMethod, newRoute.order, newRoute.compiledPattern, newRoute.body, routeMethod, newRoute.cacheSeconds)
    (controller, withRouteMethod)
  }

  /** For postback to create controller from "controllerName#routeName" */
  def newControllerAndRoute(controllerRouteName: String): (Controller, Route) = {
    val (controllerName, routeName) = splitControllerRouteName(controllerRouteName)
    val routeo = getRouteMethod(controllerName, routeName, false) // No need to cache, because it has already been cached
    newControllerAndRoute(routeo.get)
  }

  /**
   * Routes in controller companion object will have null routeMethod.
   * To create new controller instances or get controller class name & route name,
   * these routes need lookup this map to get non-null routeMethods.
   */
  private def cacheRouteMethodToRouteInCompanionControllerObject(controllerClassName: String, routeName: String, routeMethod: Method) {
    // If the controller class has no companion object,
    // ClassNotFoundException will be thrown
    try {
      val companionClass = classResolver.resolve(controllerClassName + "$")
      val rm             = companionClass.getMethod(routeName)
      val MODULE$Field   = companionClass.getField("MODULE$")
      val MODULE$        = MODULE$Field.get(null)
      val route          = rm.invoke(MODULE$).asInstanceOf[Route]
      route.routeMethod  = routeMethod
    } catch {
      case _ =>
    }
  }
}
