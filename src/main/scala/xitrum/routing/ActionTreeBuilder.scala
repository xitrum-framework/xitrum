package xitrum.routing

import java.lang.reflect.Modifier
import scala.collection.mutable.{ArrayBuffer, Map => MMap}
import scala.reflect.runtime.universe

import xitrum.{Action, Config}
import xitrum.annotation.ActionAnnotations
import xitrum.util.ClassFileLoader

/**
 * This class is intended to be used by RouteCollector and to be serialized to
 * routes.cache. xitrumVersion is saved in routes.cache, so that when we load
 * routes.cache later, we can check if Xitrum version has changed.
 *
 * For each .class file, RouteCollector calls "addBranches" to pass its class
 * name, super class name, and interface names. At this step, we build trees of
 * parent -> children.
 *
 * Lastly, RouteCollector calls "getConcreteActionsAndAnnotations" to get
 * concrete (non-trait, non-abstract) action classes and their annotations.
 *
 * @param parent2Children Map to save trees; keys and values are class names
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
    // We can't use ASM or Javassist to get annotations, because they don't
    // understand Scala annotations.

    val concreteActions = getConcreteActions

    // Below:
    // * In development mode, we must use a new throwaway class loader, so that
    //   modified annotations (if any) can be recollected.
    // * We use class name instead of class, to avoid conflict between different
    //   class loaders.
    val cl            = if (Config.productionMode) Thread.currentThread.getContextClassLoader else new ClassFileLoader
    val actionClass   = cl.loadClass(classOf[Action].getName)
    val runtimeMirror = universe.runtimeMirror(cl)
    val cache         = MMap.empty[String, ActionAnnotations]

    def getActionAccumulatedAnnotations(className: String): ActionAnnotations = {
      cache.get(className) match {
        case Some(aa) => aa

        case None =>
          val klass             = cl.loadClass(className)
          val parentClasses     = Seq(klass.getSuperclass) ++ klass.getInterfaces
          val parentAnnotations = parentClasses.foldLeft(ActionAnnotations()) { case (acc, parentClass) =>
            // parentClass is null if klass is a trait/interface
            if (parentClass == null) {
              acc
            } else if (actionClass.isAssignableFrom(parentClass)) {
              val aa = getActionAccumulatedAnnotations(parentClass.getName)
              acc.inherit(aa)
            } else {
              acc
            }
          }

          val universeAnnotations = runtimeMirror.classSymbol(klass).asClass.annotations
          val thisAnnotationsOnly = ActionAnnotations.fromUniverse(universeAnnotations)
          val ret                 = thisAnnotationsOnly.inherit(parentAnnotations)
          cache(className)        = ret
          ret
      }
    }

    concreteActions.map { ca => (ca, getActionAccumulatedAnnotations(ca.getName)) }
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
          val klass    = Thread.currentThread.getContextClassLoader.loadClass(className)
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
