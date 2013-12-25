package xitrum.view

import xitrum.Action

trait GetActionClassDefaultsToCurrentAction {
  this: Action =>

  def getActionClass[T <: Action : Manifest]: Class[Action] = {
    val klass = manifest[T].runtimeClass.asInstanceOf[Class[Action]]
    if (klass == classOf[Nothing]) getClass.asInstanceOf[Class[Action]] else klass
  }
}
