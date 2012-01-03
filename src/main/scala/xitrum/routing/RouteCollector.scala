package xitrum.routing

import java.io.{ByteArrayInputStream, DataInputStream, File}
import java.lang.reflect.Method
import java.util.{List => JList}

import scala.collection.JavaConversions

import sclasner.{FileEntry, Scanner}
import javassist.bytecode.{ClassFile, FieldInfo}

import xitrum.{Config, Logger}

/** Scan all classes to collect routes from controllers. */
class RouteCollector(cachedFileName: String) extends Logger {
  /** @return Action methods grouped by controllers */
  def fromCacheFileOrRecollect(): Seq[Seq[Method]] = {
    logger.info("Load " + cachedFileName + "/recollect routes and action/page cache config from cotrollers...")

    try {
      Scanner.foldLeft(cachedFileName, Seq[Seq[Method]](), discovered _)
    } catch {
      case e =>
        // Maybe routes.sclasner could not be loaded because dependencies have changed.
        // Try deleting routes.sclasner and scan again.
        val f = new File(cachedFileName)
        if (f.exists) {
          logger.warn("Error loading " + cachedFileName + ". Delete the file and recollect routes...")
          f.delete()
          try {
            Scanner.foldLeft(cachedFileName, Seq[Seq[Method]](), discovered _)
          } catch {
            case e2 =>
              Config.exitOnError("Could not collect routes", e2)
              throw e2
          }
        } else {
          Config.exitOnError("Could not collect routes", e)
          throw e
        }
    }
  }

  //----------------------------------------------------------------------------

  private def discovered(acc: Seq[Seq[Method]], entry: FileEntry): Seq[Seq[Method]] = {
    try {
      if (entry.relPath.endsWith(".class")) {
        val bais = new ByteArrayInputStream(entry.bytes)
        val dis  = new DataInputStream(bais)
        val cf   = new ClassFile(dis)
        dis.close()

        val className  = cf.getName
        if (className.endsWith("$")) {  // Ignore Scala objects
          acc
        } else {
          val fieldInfoList = cf.getFields.asInstanceOf[JList[FieldInfo]]
          if (fieldInfoList == null) {
            acc
          } else {
            val routeMethods = JavaConversions.asScalaBuffer(fieldInfoList).foldLeft(Seq[Method]()) { (acc2, fi) =>
              if (fi.getDescriptor == routeClassDescriptor) {
                val methodName = fi.getName  // Scala "val" creates method with the same name
                ControllerReflection.getRouteMethod(className, methodName) match {
                  case None => acc2
                  case Some(routeMethod) => acc2 :+ routeMethod
                }
              } else {
                acc2
              }
            }
            if (routeMethods.isEmpty) acc else acc :+ routeMethods
          }
        }
      } else {
        acc
      }
    } catch {
      case e =>
        logger.debug("Could not scan route for " + entry.relPath + " in " + entry.container, e)
        acc
    }
  }

  private lazy val routeClassDescriptor = {
    // Something like "Lxitrum/routing/Route;"
    "L" + classOf[Route].getName.replace('.', '/') + ";"
  }
}
