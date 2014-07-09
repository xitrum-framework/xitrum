package xitrum.i18n

import java.net.{URL, URLClassLoader}
import java.nio.file.{Files, Path, Paths}
import scala.collection.mutable.{ListBuffer, Map => MMap}

import io.netty.util.CharsetUtil.UTF_8
import scaposer.{Po, Parser}

import xitrum.Log
import xitrum.util.{FileMonitor, Loader}

object PoLoader {
  private val cache    = MMap.empty[String, Po]
  private val watching = MMap.empty[Path, Boolean]
  watch()

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
  def get(language: String): Po = {
    if (cache.isDefinedAt(language)) return cache(language)

    synchronized {
      val urlEnum = Thread.currentThread.getContextClassLoader.getResources("i18n/" + language + ".po")
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

  /**
   * Reloads all loaded po files of the specified language in the cache.
   */
  def reload(language: String) {
    Log.info("Reload po file of language: " + language)
    remove(language)
    get(language)
  }

  /**
   * Watches i18n directories in classpath to reload po files automatically.
   */
  private def watch() {
    val classPath = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs
    classPath.foreach { cp =>
      val withI18n = new URL(cp, "i18n")
      val i18nPath = Paths.get(withI18n.toURI)
      if (!watching.isDefinedAt(i18nPath) && Files.isDirectory(i18nPath)) {
        watching(i18nPath) = true
        Log.info("Monitor po files in: " + i18nPath)
        FileMonitor.monitor(FileMonitor.MODIFY, i18nPath, { path =>
          val fileName = path.getFileName.toString
          if (fileName.endsWith(".po")) {
            val lang = fileName.substring(0, fileName.length - ".po".length)
            reload(lang)
          }
        })
      }
    }
  }
}
