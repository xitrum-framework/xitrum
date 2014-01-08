package xitrum.i18n

import scala.collection.mutable.{ListBuffer, Map => MMap}

import org.jboss.netty.util.CharsetUtil.UTF_8
import scaposer.{Po, Parser}

import xitrum.util.Loader

object PoLoader {
  private[this] val cache = MMap.empty[String, Po]

  /**
   * For the specified language, this method loads and merges all po files in
   * classpath, at i18n/<language>.po. You can store the po files in JAR files,
   * at src/main/resources/i18n/<language>.po (compile time), or at
   * config/i18n/<language>.po ("config" directory is put to classpath by Xitrum
   * at run time, this is convenient if you want to modify po files at run time).
   *
   * The result is stored in cache for further fast access. If you want to reload
   * the po files, use clear() or remove(language) to clean the cache, then load again.
   *
   * @return Empty Po if there's no po file
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

  /**
   * Clears all loaded po files of all languages in the cache.
   */
  def clear() = synchronized {
    cache.clear()
  }

  /**
   * Clears all loaded po files of the specified language in the cache.
   */
  def remove(language: String) = synchronized {
    cache.remove(language)
  }
}
