package xitrum.i18n

import java.io.File
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import scaposer.{Parser, I18n}
import sclasner.Discoverer

import xitrum.Log
import xitrum.util.{FileMonitor, Loader}

object PoLoader {
  // Also watch this directory in development mode
  private val DEV_RESOURCES_DIR = "src/main/resources"

  private val cache = MMap.empty[String, I18n]
  watch()

  /**
   * For the specified language, this method loads and merges all po files in
   * classpath and src/main/resources, at i18n/<language>.po.
   *
   * You can store the po files in JAR files, at src/main/resources/i18n/<language>.po
   * (compile time), or at config/i18n/<language>.po ("config" directory is put
   * to classpath by Xitrum at run time, this is convenient if you want to modify
   * po files at run time without having to recompile or restart).
   *
   * The result is stored in cache for further fast access. If you want to reload
   * the po files, use clear() or remove(language) to clean the cache, then load again.
   *
   * @return Empty Po if there's no po file
   */
  def get(language: String): I18n = {
    if (cache.isDefinedAt(language)) return cache(language)

    synchronized {
      val urlEnum = Thread.currentThread.getContextClassLoader.getResources("i18n/" + language + ".po")
      val buffer  = ArrayBuffer.empty[I18n]
      while (urlEnum.hasMoreElements) {
        val url    = urlEnum.nextElement()
        val is     = url.openStream()
        val string = Loader.stringFromInputStream(is)
        Parser.parse(string) match {
          case Left(parseFailure) =>
            Log.warn(s"Could not load $url: $parseFailure")

          case Right(translations) =>
            val i18n = I18n(translations)
            buffer.append(i18n)
        }
      }

      val file = new File(DEV_RESOURCES_DIR + "/i18n/" + language + ".po")
      if (file.exists) {
        val string = Loader.stringFromFile(file)
        Parser.parse(string) match {
          case Left(parseFailure) =>
            Log.warn(s"Could not load $file: $parseFailure")

          case Right(translations) =>
            val i18n = I18n(translations)
            buffer.append(i18n)
        }
      }

      val ret = buffer.foldLeft(new I18n(Map.empty)) { (acc, e) => acc ++ e }
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
   * Watches i18n directories in classpath and src/main/resources
   * (for development mode) to reload .po files automatically.
   */
  private def watch() {
    val searchDirs = Discoverer.containers.filter(_.isDirectory) ++ Seq(new File(DEV_RESOURCES_DIR))
    searchDirs.foreach { dir =>
      val withI18n = new File(dir, "i18n")
      if (withI18n.exists && withI18n.isDirectory) {
        val i18nPath = withI18n.toPath
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
