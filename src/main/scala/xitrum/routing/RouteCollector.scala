package xitrum.routing

import java.io.{ByteArrayInputStream, DataInputStream}
import java.lang.reflect.Method
import java.util.{List => JList}

import scala.collection.JavaConversions

import sclasner.{FileEntry, Scanner}
import javassist.bytecode.{ClassFile, FieldInfo, AccessFlag}

import xitrum.{Config, Logger}
import xitrum.controller.Action

/** Scan all classes to collect routes from controllers. */
class RouteCollector(cachedFileName: String) extends Logger {
  /**
   * Because java.lang.reflect.Method is not serializable, we return a map of
   * controller class name -> route method names.
   */
  def fromCacheFileOrRecollect(): Map[String, Seq[String]] =
    Scanner.foldLeft(cachedFileName, Map[String, Seq[String]](), discovered _)

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
            val actionNames = JavaConversions.asScalaBuffer(fieldInfoList).foldLeft(Seq[String]()) { (acc2, fi) =>
              if (fi.getDescriptor == actionClassDescriptor) {
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
            if (actionNames.isEmpty) acc else acc + (className -> actionNames)
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

  private lazy val actionClassDescriptor = {
    // Something like "Lxitrum/controller/Action;"
    "L" + classOf[Action].getName.replace('.', '/') + ";"
  }
}
