package xitrum.util

import java.io.File
import scala.collection.mutable.{Map => MMap}

/**
 * This utility is useful for hot reloading .class files in a directory during
 * development.
 *
 * @param classesDirectory Example: target/scala-2.11/classes
 * @param fallback Class loader for loading other classes, not inside classesDirectory
 */
class ClassFileLoader(classesDirectory: String) extends ClassLoader {
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
              val bytes = Loader.bytesFromFile(path)
              val klass = defineClass(name, bytes, 0, bytes.length)
              cache(name) = klass
              klass
            }
        }
    }
  }

  //----------------------------------------------------------------------------

  protected def fallback: ClassLoader = getClass.getClassLoader

  protected def classNameToFilePath(name: String): Option[String] = {
    if (useFallback(name)) return None

    val path = classesDirectory + File.separator + name.replaceAllLiterally(".", File.separator) + ".class"
    Some(path)
  }

  protected def useFallback(name: String): Boolean = {
    // Do not reload Scala objects:
    // https://github.com/xitrum-framework/xitrum/issues/418
    if (name.endsWith("$")) return true

    false
  }
}
