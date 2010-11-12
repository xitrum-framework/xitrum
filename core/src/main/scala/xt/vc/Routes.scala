package xt.vc

import org.reflections.Reflections
import org.reflections.util.{ConfigurationBuilder, ClasspathHelper}

object Routes {
  def scan {
    val cb = new ConfigurationBuilder
    cb.setUrls(ClasspathHelper.getUrlsForCurrentClasspath)
    val r = new Reflections(cb)

    val cs = r.getSubTypesOf(classOf[Controller])
    val ic = cs.iterator
    while (ic.hasNext) {
      val c = ic.next
      val ms = c.getMethods
      for (m <- ms) {
        val as = m.getAnnotations
        for (a <- as) {
          if (a.isInstanceOf[Path]) {
            val p = a.asInstanceOf[Path]
            println(p.value)
          }
        }
      }
    }
  }
}
