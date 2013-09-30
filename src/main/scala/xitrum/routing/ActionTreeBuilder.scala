package xitrum.routing

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe

import xitrum.Action
import xitrum.annotation.ActionAnnotations

/**
 * Intended for use by RouteCollector.
 *
 * For each .class file, RouteCollector uses ASM's ClassReader to load it, then
 * calls "addBranches" to pass its class name and interface names. Action is a
 * trait. Traits are seen by ASM as interfaces. At this step, we build trees of
 * interface -> children.
 *
 * Lastly, RouteCollector calls "getConcreteActionsAndAnnotations" to get
 * concrete (non-trait) action classes and their annotations.
 *
 * This class is immutable because it will be serialized to routes.cache.
 *
 * @param interface2Children Map to save trees; names are class names
 */
private case class ActionTreeBuilder(
    interface2Children: Map[String, Seq[String]] = Map()
) {
  def addBranches(
      childInternalName:   String,
      parentInternalNames: Array[String]
  ): ActionTreeBuilder = {
    // Class name: xitrum.Action
    // Internal name: xitrum/Action
    def internalName2ClassName(internalName: String) = internalName.replace('/', '.')

    if (parentInternalNames == null) return this

    val childClassName = internalName2ClassName(childInternalName)

    var i2c = interface2Children
    parentInternalNames.foreach { parentInternalName =>
      val parentClassName = internalName2ClassName(parentInternalName)
      if (interface2Children.isDefinedAt(parentClassName)) {
        val children    = interface2Children(parentClassName)
        val newChildren = children :+ childClassName
        i2c += parentClassName -> newChildren
      } else {
        i2c += parentClassName -> Seq(childClassName)
      }
    }

    ActionTreeBuilder(i2c)
  }

  /**
   * Annotations of action ancestors will be pushed down to descedants. If a
   * parent declares cache info, its descedants will have that cache info.
   * Descedants may override annotations of ancestors. If ancestor declares
   * cache time of 5 minutes, but descedant declares 10 minutes, descedant will
   * have 10 minutes in effect.
   *
   * @return Concrete (non-trait) action classes and their annotations
   */
  def getConcreteActionsAndAnnotations: Set[(Class[_ <: Action], ActionAnnotations)] = {
    val concreteActions = getConcreteActions

    val runtimeMirror = universe.runtimeMirror(getClass.getClassLoader)
    var cache         = Map[Class[_ <: Action], ActionAnnotations]()

    def getActionAccumulatedAnnotations(klass: Class[_ <: Action]): ActionAnnotations = {
      cache.get(klass) match {
        case Some(aa) => aa

        case None =>
          val parents = klass.getInterfaces
          val parentAnnotations = parents.foldLeft(ActionAnnotations()) { case (acc, parent) =>
            if (parent.isAssignableFrom(classOf[Action])) {
              val aa = getActionAccumulatedAnnotations(parent.asInstanceOf[Class[_ <: Action]])
              acc.overrideMe(aa)
            } else {
              acc
            }
          }

          val annotations = runtimeMirror.classSymbol(klass).asClass.annotations
          val ret         = parentAnnotations.overrideMe(annotations)

          cache += klass -> ret
          ret
      }
    }

    concreteActions.map { ca => (ca, getActionAccumulatedAnnotations(ca)) }
  }

  //----------------------------------------------------------------------------

  private def getConcreteActions: Set[Class[_ <: Action]] = {
    var concreteActions = Set[Class[_ <: Action]]()

    def traverseActionTree(className: String) {
      if (interface2Children.isDefinedAt(className)) {
        val children = interface2Children(className)
        children.foreach { className =>
          val klass = Class.forName(className).asInstanceOf[Class[_ <: Action]]
          if (!klass.isInterface) concreteActions += klass  // Not interface => concrete
          traverseActionTree(className)
        }
      }
    }

    // We are only interested in the Action tree
    val actionRoot = classOf[Action].getName
    traverseActionTree(actionRoot)
    concreteActions
  }
}
