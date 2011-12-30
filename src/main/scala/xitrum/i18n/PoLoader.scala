package xitrum.i18n

import scala.collection.mutable.{ListBuffer, Map => MMap}

import io.netty.util.CharsetUtil.UTF_8
import scaposer.{Po, Parser}

import xitrum.util.Loader

object PoLoader {
  private val cache = MMap[String, Po]()

  /**
   * @return Merge of all po files of the language, or an empty Po when there's
   * no po file.
   */
  def load(language: String): Po = synchronized {
    if (cache.isDefinedAt(language)) return cache(language)

    val urlEnum = getClass.getClassLoader.getResources("i18n/" + language + ".po")
    val buffer  = ListBuffer[Po]()
    while (urlEnum.hasMoreElements) {
      val url    = urlEnum.nextElement
      val is     = url.openStream
      val bytes  = Loader.bytesFromInputStream(is)
      val string = new String(bytes, UTF_8)
      val poo    = Parser.parsePo(string)
      if (poo.isDefined) buffer.append(poo.get)
    }

    val ret = if (buffer.isEmpty) {
      new Po(Map())
    } else {
      buffer.reduce { (po1, po2) => po1 ++ po2 }
    }

    cache(language) = ret
    ret
  }
}
