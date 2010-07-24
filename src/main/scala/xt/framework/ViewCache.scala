package xt.framework

import java.lang.reflect.Method

import scala.collection.mutable.HashMap
import scala.xml.NodeSeq

/**
 * Note: cuncurrenct access
 */
object ViewCache {
  var viewPaths: List[String] = _

  private val cache = new HashMap[String, java.lang.Class[View]]

  /**
   * Given "Articles#index":
   * + Creates an instance of xxx.articles.Index
   * + Sets things from helper to it
   * + Then calls its render method
   */
  def renderView(csas: String, helper: Helper): NodeSeq = {
    // Create a view instance
    val view = cache.get(csas) match {
      case Some(k) => k.newInstance

      case None =>
        val caa = csas.split("#")
        val cs1 = caa(0)           // Articles
        val as1 = caa(1)           // index
        val cs2 = cs1.toLowerCase  // articles
        val as2 = as1.capitalize   // Index
        val end = cs2 + "." + as2  // articles.Index

        var k: Class[View] = null
        viewPaths.find { p =>
          val fp = p + "." + end
          try {
            k = Class.forName(fp).asInstanceOf[Class[View]]
            true
          } catch {
            case _ => false
          }
        }
        if (k == null) throw(new Exception("Could not load " + csas))

        k.newInstance
    }

    view.setRefs(helper)
    view.render
  }
}
