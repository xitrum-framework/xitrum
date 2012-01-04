package xitrum.routing

import java.io.{ByteArrayInputStream, DataInputStream, File}
import java.lang.reflect.Method
import java.util.{List => JList}

import scala.collection.JavaConversions

import sclasner.{FileEntry, Scanner}
import javassist.bytecode.{ClassFile, FieldInfo, AccessFlag}

import xitrum.{Config, Logger}

/** Scan all classes to collect routes from controllers. */
class RouteCollector(cachedFileName: String) extends Logger {
  /**
   * Because java.lang.reflect.Method is not serializable, we return a map of
   * controller class name -> route method names.
   */
  def fromCacheFileOrRecollect(): Map[String, Seq[String]] = {
    logger.info("Load " + cachedFileName + "/recollect routes and action/page cache config from cotrollers...")

    try {
      Scanner.foldLeft(cachedFileName, Map[String, Seq[String]](), discovered _)
    } catch {
      case e =>
        // Maybe routes.sclasner could not be loaded because dependencies have changed.
        // Try deleting routes.sclasner and scan again.
        val f = new File(cachedFileName)
        if (f.exists) {
          logger.warn("Error loading " + cachedFileName + ". Delete the file and recollect routes...")
          f.delete()
          try {
            Scanner.foldLeft(cachedFileName, Map[String, Seq[String]](), discovered _)
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

  private def discovered(acc: Map[String, Seq[String]], entry: FileEntry): Map[String, Seq[String]] = {
    try {
      if (entry.relPath.endsWith(".class")) {
        val bais = new ByteArrayInputStream(entry.bytes)
        val dis  = new DataInputStream(bais)
        val cf   = new ClassFile(dis)
        dis.close()

        val className  = cf.getName
        if (className.contains("$")) {  // Ignore Scala objects
          acc
        } else {
          val fieldInfoList = cf.getFields.asInstanceOf[JList[FieldInfo]]
          if (fieldInfoList == null) {
            acc
          } else {
            val routeMethodNames = JavaConversions.asScalaBuffer(fieldInfoList).foldLeft(Seq[String]()) { (acc2, fi) =>
              if (fi.getDescriptor == routeClassDescriptor) {
                val methodName = fi.getName  // Scala "val" creates method with the same name
                val mi = cf.getMethod(methodName)
                if (mi == null) {
                  acc2
                } else {
                  // This method should be public but not static
                  val accessFlags = mi.getAccessFlags
                  val isPublic = (accessFlags & AccessFlag.PUBLIC) == 1
                  val isStatic = (accessFlags & AccessFlag.STATIC) == 1
                  if (isPublic && !isStatic) acc2 :+ methodName else acc2
                }
              } else {
                acc2
              }
            }
            if (routeMethodNames.isEmpty) acc else acc + (className -> routeMethodNames)
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
