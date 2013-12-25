package xitrum.i18n

import scala.collection.mutable.{ListBuffer, Map => MMap}

import org.jboss.netty.util.CharsetUtil.UTF_8
import scaposer.{Po, Parser}

import xitrum.util.Loader

object PoLoader {
  private[this] val cache = MMap.empty[String, Po]

  /**
   * @return Merge of all po files of the language, or an empty Po when there's
   * no po file.
   */
  def load(language: String): Po = synchronized {
    if (cache.isDefinedAt(language)) return cache(language)

    val urlEnum = getClass.getClassLoader.getResources("i18n/" + language + ".po")
    val buffer  = ListBuffer.empty[Po]
    while (urlEnum.hasMoreElements) {
      val url    = urlEnum.nextElement
      val is     = url.openStream
      val bytes  = Loader.bytesFromInputStream(is)
      val string = new String(bytes, UTF_8)
      Parser.parsePo(string).foreach(buffer.append(_))
    }

    val ret = buffer.foldLeft(new Po(Map.empty)) { (acc, e) => acc ++ e }
    cache(language) = ret
    ret
  }
}
