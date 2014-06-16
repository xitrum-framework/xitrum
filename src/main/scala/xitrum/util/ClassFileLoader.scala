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
class ClassFileLoader(classesDirectory: String, fallback: ClassLoader) extends ClassLoader(fallback) {
  private val cache = MMap[String, Class[_]]()

  override def loadClass(name: String): Class[_] = {
    findClass(name)
  }

  override def findClass(name: String): Class[_] = synchronized {
    cache.get(name) match {
      case Some(klass) =>
        klass

      case None =>
        loadClassData(name) match {
          case None =>
            fallback.loadClass(name)

          case Some(bytes) =>
            val klass = defineClass(name, bytes, 0, bytes.length)
            cache(name) = klass
            klass
        }
    }
  }

  private def loadClassData(name: String): Option[Array[Byte]] = {
    val path = classesDirectory + File.separator + name.replaceAllLiterally(".", File.separator) + ".class"
    val file = new File(path)
    if (file.exists) Some(Loader.bytesFromFile(path)) else None
  }
}
