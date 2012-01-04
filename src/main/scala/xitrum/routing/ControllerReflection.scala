package xitrum.routing

import java.lang.reflect.Method
import scala.collection.mutable.{Map => MMap}

import io.netty.handler.codec.serialization.ClassResolvers

import xitrum.Controller

object ControllerReflection {
  private val classResolver = ClassResolvers.softCachingConcurrentResolver(getClass.getClassLoader)

  // Routes in controller companion object will have null routeMethod.
  // To create new controller instances or get controller class name & route name,
  // these routes need lookup this map to get non-null routeMethods.
  private val routeWithNullRouteMethod_to_RouteMethod = MMap[Route, Method]()

  /** @return controller#route */
  def controllerRouteName(route: Route): String =
    controllerRouteName(route.routeMethod)

  def controllerRouteName(routeMethod: Method): String = {
    val controllerClassName = routeMethod.getDeclaringClass.getName
    val routeName           = routeMethod.getName
    controllerClassName + "#" + routeName
  }

  def splitControllerRouteName(controllerRouteName: String): (String, String) = {
    val array = controllerRouteName.split('#')
    (array(0), array(1))
  }

  /** Called by RouteCollector */
  def getRouteMethod(className: String, methodName: String): Option[Method] = {
    val klass = classResolver.resolve(className)
    if (classOf[Controller].isAssignableFrom(klass)) {
      val routeMethod = klass.getMethod(methodName)
      storeRouteMethodForRouteWithNullRouteMethodToLookupLater(className, methodName, routeMethod)
      Some(routeMethod)
    } else {
      None
    }
  }

  def newControllerAndRoute(route: Route): (Controller, Route) = {
    val routeMethod        = route.routeMethod
    val nonNullRouteMethod = if (routeMethod == null) lookupRouteMethodForRouteWithNullRouteMethod(route) else routeMethod
    newControllerAndRoute(nonNullRouteMethod)
  }

  def newControllerAndRoute(nonNullRouteMethod: Method): (Controller, Route) = {
    val controllerClass = nonNullRouteMethod.getDeclaringClass
    val controller      = controllerClass.newInstance().asInstanceOf[Controller]
    val newRoute        = nonNullRouteMethod.invoke(controller).asInstanceOf[Route]

    val withRouteMethod = Route(newRoute.httpMethod, newRoute.order, newRoute.compiledPattern, newRoute.body, nonNullRouteMethod, newRoute.cacheSeconds)
    (controller, withRouteMethod)
  }

  def newControllerAndRoute(controllerRouteName: String): (Controller, Route) = {
    val (controllerName, routeName) = splitControllerRouteName(controllerRouteName)
    val routeo = getRouteMethod(controllerName, routeName)
    newControllerAndRoute(routeo.get)
  }

  /** Called by newControllerAndRoute and RouteFactory#nonNullRouteMethodFromRoute */
  def lookupRouteMethodForRouteWithNullRouteMethod(route: Route): Method = {
    routeWithNullRouteMethod_to_RouteMethod.get(route) match {
      case Some(method) => method
      case None => null
    }
  }

  private def storeRouteMethodForRouteWithNullRouteMethodToLookupLater(controllerClassName: String, routeName: String, routeMethod: Method) {
    // If the controller class has no companion object,
    // ClassNotFoundException will be thrown
    try {
      val companionClass = classResolver.resolve(controllerClassName + "$")

      val rm = companionClass.getMethod(routeName)
      val MODULE$Field = companionClass.getField("MODULE$")
      val MODULE$ = MODULE$Field.get(null)
      val route = rm.invoke(MODULE$).asInstanceOf[Route]

      routeWithNullRouteMethod_to_RouteMethod(route) = routeMethod
    } catch {
      case _ =>
    }
  }
}
