package xitrum.routing

import java.lang.reflect.Modifier
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe

import xitrum.Action
import xitrum.annotation.ActionAnnotations

/**
 * Intended for use by RouteCollector.
 *
 * For each .class file, RouteCollector uses ASM's ClassReader to load it, then
 * calls "addBranches" to pass its class name, super class name, and interface names.
 * At this step, we build trees of parent -> children.
 *
 * Lastly, RouteCollector calls "getConcreteActionsAndAnnotations" to get
 * concrete (non-trait, non-abstract) action classes and their annotations.
 *
 * This class is immutable because it will be serialized to routes.cache.
 *
 * @param parent2Children Map to save trees; names are class names
 */
private case class ActionTreeBuilder(xitrumVersion: String, parent2Children: Map[String, Seq[String]] = Map.empty) {
  def addBranches(
      childInternalName:      String,
      superInternalName:      String,
      interfaceInternalNames: Array[String]
  ): ActionTreeBuilder = {
    if (superInternalName == null || interfaceInternalNames == null) return this

    // Optimize: Ignore Java and Scala default classes; these can be thousands
    if (childInternalName.startsWith("java/")  ||
        childInternalName.startsWith("javax/") ||
        childInternalName.startsWith("scala/") ||
        childInternalName.startsWith("sun/")   ||
        childInternalName.startsWith("com/sun/")) return this

    val parentInternalNames = Seq(superInternalName) ++ interfaceInternalNames
    val parentClassNames    = parentInternalNames.map(internalName2ClassName _)

    val childClassName = internalName2ClassName(childInternalName)
    val p2c            = parentClassNames.foldLeft(parent2Children) { case (acc, parentClassName) =>
      if (acc.isDefinedAt(parentClassName)) {
        val children    = parent2Children(parentClassName)
        val newChildren = children :+ childClassName
        acc + (parentClassName -> newChildren)
      } else {
        acc + (parentClassName -> Seq(childClassName))
      }
    }

    ActionTreeBuilder(xitrumVersion, p2c)
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
    var cache         = Map.empty[Class[_ <: Action], ActionAnnotations]

    def getActionAccumulatedAnnotations(klass: Class[_ <: Action]): ActionAnnotations = {
      cache.get(klass) match {
        case Some(aa) => aa

        case None =>
          val parentClasses     = Seq(klass.getSuperclass) ++ klass.getInterfaces
          val parentAnnotations = parentClasses.foldLeft(ActionAnnotations()) { case (acc, parentClass) =>
            // parentClass is null if klass is a trait/interface
            if (parentClass == null) {
              acc
            } else if (classOf[Action].isAssignableFrom(parentClass)) {
              val aa = getActionAccumulatedAnnotations(parentClass.asInstanceOf[Class[_ <: Action]])
              acc.inherit(aa)
            } else {
              acc
            }
          }

          val universeAnnotations = runtimeMirror.classSymbol(klass).asClass.annotations
          val thisAnnotationsOnly = ActionAnnotations.fromUniverse(universeAnnotations)
          val ret                 = thisAnnotationsOnly.inherit(parentAnnotations)
          cache += klass -> ret
          ret
      }
    }

    concreteActions.map { ca => (ca, getActionAccumulatedAnnotations(ca)) }
  }

  //----------------------------------------------------------------------------

  // Class name: xitrum.Action
  // Internal name: xitrum/Action
  private def internalName2ClassName(internalName: String) = internalName.replace('/', '.')

  private def getConcreteActions: Set[Class[_ <: Action]] = {
    var concreteActions = Set.empty[Class[_ <: Action]]

    def traverseActionTree(className: String) {
      if (parent2Children.isDefinedAt(className)) {
        val children = parent2Children(className)
        children.foreach { className =>
          val klass    = Class.forName(className)
          val concrete = !klass.isInterface && !Modifier.isAbstract(klass.getModifiers)
          if (concrete) concreteActions += klass.asInstanceOf[Class[_ <: Action]]
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
