package xitrum.routing

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe

import xitrum.Action

/**
 * Intended for use by RouteCollector.
 *
 * For each .class file, RouteCollector uses ASM's ClassReader to load it, then
 * calls "addBranches" to pass its class name and interface names. Action is a
 * trait. Traits are seen by ASM as interfaces. At this step, we build trees of
 * interface -> children.
 *
 * Lastly, RouteCollector calls "traverse" to walk through all direct and
 * indirect children of Action.
 *
 * This class is immutable because it will be serialized to routes.cache.
 *
 * @param interface2Children Map to save trees
 */
private case class ActionTreeBuilder(interface2Children: Map[String, Seq[String]] = Map()) {
  // Normal name: xitrum.Action
  // Internal name: xitrum/Action
  def addBranches(
      childInternalName:   String,
      parentInternalNames: Array[String]
  ): ActionTreeBuilder = {
    if (parentInternalNames == null) return this

    var ret = interface2Children
    parentInternalNames.foreach { p =>
      if (interface2Children.isDefinedAt(p)) {
        val children    = interface2Children(p)
        val newChildren = children :+ childInternalName
        ret += p -> newChildren
      } else {
        ret += p -> Seq(childInternalName)
      }
    }

    ActionTreeBuilder(ret)
  }

  /**
   * @param annotationsProcessor Will be passed a concrete (non-trait) Action class
   * and a non-empty collection of annotations. Annotations of ancestors will be
   * pushed down to descedants. If a parent declares cache info, its descedants
   * will have that cache info. However, descedants may override annotations of
   * ancestors. For example, if ancestor declares cache time of 5 minutes, but
   * descedant declares 10 minutes, descedant will have 10 minutes in effect.
   */
  def traverse(annotationsProcessor: (Class[_ <: Action], Seq[universe.Annotation]) => Unit) {
    // We are only interested in the Action tree
    val actionRootInternalName = classOf[xitrum.Action].getName.replace('.', '/')
    val runtimeMirror          = universe.runtimeMirror(getClass.getClassLoader)
    preOrderTraverse(runtimeMirror, Seq(), actionRootInternalName, annotationsProcessor)
  }

  //----------------------------------------------------------------------------

  private def preOrderTraverse(
      runtimeMirror:        universe.Mirror,
      ancestorsAnnotations: Seq[universe.Annotation],
      key:                  String,
      annotationsProcessor: (Class[_ <: Action], Seq[universe.Annotation]) => Unit)
  {
    if (interface2Children.isDefinedAt(key)) {
      val children = interface2Children(key)
      children.foreach { internalName =>
        val className            = internalName.replace('/', '.')
        val klass                = Class.forName(className).asInstanceOf[Class[_ <: Action]]
        val annotations          = runtimeMirror.classSymbol(klass).asClass.annotations
        val effectiveAnnotations = overrideAncestorsAnnotations(ancestorsAnnotations, annotations)

        if (!klass.isInterface && annotations.nonEmpty)
          annotationsProcessor(klass, effectiveAnnotations)

        preOrderTraverse(runtimeMirror, effectiveAnnotations, internalName, annotationsProcessor)
      }
    }
  }

  /**
   * Only overrides cache info. App developers should be responsible for ensuring
   * the correctness of their routes.
   */
  private def overrideAncestorsAnnotations(
      ancestorsAnnotations: Seq[universe.Annotation],
      annotations:          Seq[universe.Annotation]
  ): Seq[universe.Annotation] = {
    val hadCacheAnnotation = annotations.exists(isCacheAnnotation(_))
    if (hadCacheAnnotation) {
      val noCacheAnnotations = ancestorsAnnotations.filterNot(isCacheAnnotation _)
      noCacheAnnotations ++ annotations
    } else {
      ancestorsAnnotations ++ annotations
    }
  }

  private def isCacheAnnotation(annotation: universe.Annotation) = {
    // FIXME: Check if annotation is from CacheAnnotation
    false
  }
}
