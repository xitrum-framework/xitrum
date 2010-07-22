package xt.framework

import java.lang.reflect.Method
import scala.collection.mutable.HashMap

/**
 * Note: cuncurrenct access
 */
object ViewCache {
  var viewPaths: List[String] = _

  private val cache = new HashMap[String, java.lang.Class[View]]

  /**
   * Given "Articles#index", returns an instance of xxx.articles.Index.
   */
  def newView(csas: String): View = {
    cache.get(csas) match {
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
          val fp = p + end
          try {
            k = Class.forName(fp).asInstanceOf[Class[View]]
            true
          } catch {
            case _ => false
          }
        }
        k.newInstance
    }
  }
}
