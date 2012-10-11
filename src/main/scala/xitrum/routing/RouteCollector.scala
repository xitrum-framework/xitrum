package xitrum.routing

import java.io.{ByteArrayInputStream, DataInputStream}
import java.lang.reflect.Method
import java.util.{List => JList}

import scala.collection.JavaConversions

import javassist.{ClassClassPath, ClassPool}
import javassist.bytecode.{ClassFile, MethodInfo, AccessFlag}
import sclasner.{FileEntry, Scanner}

import xitrum.{Config, Logger}
import xitrum.controller.Action
import xitrum.sockjs.SockJsController

/** Scan all classes to collect routes from controllers. */
class RouteCollector extends Logger {
  /**
   * Because java.lang.reflect.Method is not serializable, we return a map of
   * controller class name -> action method names.
   */
  def fromCacheFileOrRecollect(cachedFileName: String): Map[String, Seq[String]] =
    Scanner.foldLeft(cachedFileName, Map[String, Seq[String]](), discovered _)

  def fromSockJsController: Map[String, Seq[String]] = {
    val pool = ClassPool.getDefault
    pool.insertClassPath(new ClassClassPath(classOf[SockJsController]))
    val cc = pool.get(classOf[SockJsController].getName)
    discover(true, Map[String, Seq[String]](), cc.getClassFile)
  }

  //----------------------------------------------------------------------------

  private def discovered(acc: Map[String, Seq[String]], entry: FileEntry): Map[String, Seq[String]] = {
    try {
      if (entry.relPath.endsWith(".class")) {
        val bais = new ByteArrayInputStream(entry.bytes)
        val dis  = new DataInputStream(bais)
        val cf   = new ClassFile(dis)
        dis.close()
        bais.close()
        discover(false, acc, cf)
      } else {
        acc
      }
    } catch {
      case e =>
        logger.debug("Could not scan route for " + entry.relPath + " in " + entry.container, e)
        acc
    }
  }

  private def discover(fromSockJs: Boolean, acc: Map[String, Seq[String]], classFile: ClassFile): Map[String, Seq[String]] = {
    val className = classFile.getName
    if (!fromSockJs && className == classOf[SockJsController].getName) {
      acc
    } else if (className.contains("$")) {  // Ignore Scala objects
      acc
    } else {
      val methodInfoList = classFile.getMethods.asInstanceOf[JList[MethodInfo]]
      if (methodInfoList == null) {
        acc
      } else {
        val actionNames = JavaConversions.asScalaBuffer(methodInfoList).foldLeft(Seq[String]()) { (acc2, mi) =>
          if (mi.getDescriptor == actionMethodDescriptor) {
            val methodName = mi.getName

            // This method should be public but not static
            val accessFlags = mi.getAccessFlags
            val isPublic = (accessFlags & AccessFlag.PUBLIC) == 1
            val isStatic = (accessFlags & AccessFlag.STATIC) == 1
            if (isPublic && !isStatic) acc2 :+ methodName else acc2
          } else {
            acc2
          }
        }
        if (actionNames.isEmpty) acc else acc + (className -> actionNames)
      }
    }
  }

  private lazy val actionMethodDescriptor = {
    // Something like "()Lxitrum/controller/Action;"
    "()L" + classOf[Action].getName.replace('.', '/') + ";"
  }
}
