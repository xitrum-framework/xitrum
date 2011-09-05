package xitrum.i18n

import scala.collection.mutable.{ListBuffer, Map => MMap}
import xitrum.util.Loader

object PoLoader {
  private val cache = MMap[String, Option[Po]]()

  /** Merges all po files of the language. */
  def load(lang: String): Option[Po] = synchronized {
    if (cache.isDefinedAt(lang)) return cache(lang)

    val urlEnum = getClass.getClassLoader.getResources("i18n/" + lang + ".po")
    val buffer  = ListBuffer[Po]()
    while (urlEnum.hasMoreElements) {
      val url    = urlEnum.nextElement
      val is     = url.openStream
      val bytes  = Loader.bytesFromInputStream(is)
      val string = new String(bytes, "UTF-8")
      val poo    = PoParser.parsePo(string)
      if (poo.isDefined) buffer.append(poo.get)
    }

    val ret = if (buffer.isEmpty) {
      None
    } else {
      val mergedPo = buffer.reduce { (po1, po2) => po1.merge(po2) }
      Some(mergedPo)
    }
    cache(lang) = ret
    ret
  }
}
