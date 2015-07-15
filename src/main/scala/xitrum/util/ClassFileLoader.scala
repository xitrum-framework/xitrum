package xitrum.util

import java.io.File
import scala.collection.mutable.{Map => MMap}
import sclasner.Discoverer

/**
 * This utility is useful for hot reloading .class files in defined directories
 * during development.
 */
class ClassFileLoader extends ClassLoader {
  // Directories to search for .class files, example: Seq("target/scala-2.11/classes")
  private val searchDirs = Discoverer.containers.filter(_.isDirectory).map(_.toPath)

  // Need to cache because calling defineClass twice will cause exception
  protected val cache = MMap[String, Class[_]]()

  override def loadClass(className: String): Class[_] = {
    findClass(className)
  }

  override def findClass(className: String): Class[_] = synchronized {
    cache.get(className) match {
      case Some(klass) =>
        klass

      case None =>
        classNameToFilePath(className) match {
          case None =>
            Thread.currentThread.getContextClassLoader.loadClass(className)

          case Some(path) =>
            val bytes        = Loader.bytesFromFile(path)
            val klass        = defineClass(className, bytes, 0, bytes.length)
            cache(className) = klass
            klass
        }
    }
  }

  //----------------------------------------------------------------------------

  /** @return None to use the fallback ClassLoader */
  protected def classNameToFilePath(className: String): Option[String] = {
    val relPath = className.replaceAllLiterally(".", File.separator) + ".class"
    val paths   = searchDirs.map(_ + File.separator + relPath)
    paths.find(new File(_).exists)
  }
}
