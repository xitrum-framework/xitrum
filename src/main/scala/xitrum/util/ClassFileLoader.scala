package xitrum.util

import java.io.File
import scala.collection.mutable.{Map => MMap}

/**
 * This utility is useful for hot reloading .class files in a directory during
 * development.
 *
 * @param classesDirectory Example: target/scala-2.11/classes
 */
class ClassFileLoader(classesDirectory: String) extends ClassLoader(Thread.currentThread.getContextClassLoader) {
  // Need to cache because calling defineClass twice will cause exception
  protected val cache = MMap[String, Class[_]]()

  override def loadClass(name: String): Class[_] = {
    findClass(name)
  }

  override def findClass(name: String): Class[_] = synchronized {
    cache.get(name) match {
      case Some(klass) =>
        klass

      case None =>
        classNameToFilePath(name) match {
          case None =>
            fallback.loadClass(name)

          case Some(path) =>
            val file = new File(path)
            if (!file.exists) {
              fallback.loadClass(name)
            } else {
              val bytes   = Loader.bytesFromFile(path)
              val klass   = defineClass(name, bytes, 0, bytes.length)
              cache(name) = klass
              klass
            }
        }
    }
  }

  //----------------------------------------------------------------------------

  /**
   * Fallback ClassLoader used when the class file couldn't be found or when
   * class name matches ignorePattern. Default: Thread.currentThread.getContextClassLoader.
   */
  protected def fallback: ClassLoader = Thread.currentThread.getContextClassLoader

  /** @return None to use the fallback ClassLoader */
  protected def classNameToFilePath(name: String): Option[String] = {
    // Scala 2.10 only has #toString, #regex is from Scala 2.11
    if (!ignorePattern.toString.isEmpty && ignorePattern.findFirstIn(name).isDefined) {
      None
    } else {
      val path = classesDirectory + File.separator + name.replaceAllLiterally(".", File.separator) + ".class"
      Some(path)
    }
  }

  /**
   * Ignoring to reload classes that should be reloaded may cause exceptions like:
   * java.lang.ClassCastException: demos.action.Article cannot be cast to demos.action.Article
   */
  protected def ignorePattern = "".r
}
