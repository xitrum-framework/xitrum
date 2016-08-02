package xitrum.i18n

import java.io.File
import java.util.Locale
import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import scaposer.{Parser, I18n}
import sclasner.Discoverer

import xitrum.Log
import xitrum.util.{FileMonitor, Loader}

object PoLoader {
  // Also watch this directory in development mode
  private val DEV_RESOURCES_DIR = "src/main/resources"

  private val cache = MMap.empty[Locale, I18n]
  watch()

  /**
   * For the specified `locale`, this method loads and merges all `i18n/<languageTag>.po`
   * files in classpath.
   *
   * You can store the po files in JAR files, at `src/main/resources/i18n/<languageTag>.po`
   * (compile time), or at `config/i18n/<languageTag>.po` ("config" directory is put
   * to classpath by Xitrum at run time, this is convenient if you want to modify
   * po files at run time without having to recompile or restart).
   *
   * The result is stored in cache for further fast access. If you want to reload
   * the po files, use `clear()` or `remove(languageTag)` to clean the cache, then load again.
   *
   * @return Empty Po if there's no po file
   */
  def get(locale: Locale): I18n = {
    if (cache.isDefinedAt(locale)) return cache(locale)

    synchronized {
      val languageTag = locale.toLanguageTag
      val urlEnum     = Thread.currentThread.getContextClassLoader.getResources("i18n/" + languageTag + ".po")
      val buffer      = ArrayBuffer.empty[I18n]
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

      val file = new File(DEV_RESOURCES_DIR + "/i18n/" + languageTag + ".po")
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
      cache(locale) = ret
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
   * Clears all loaded po files of the specified locale in the cache.
   */
  def remove(locale: Locale) = synchronized {
    cache.remove(locale)
  }

  /**
   * Reloads all loaded po files of the specified languageTag in the cache.
   */
  def reload(locale: Locale) {
    Log.info("Reload po file of locale: " + locale)
    remove(locale)
    get(locale)
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
            val languageTag = fileName.substring(0, fileName.length - ".po".length)
            val locale = Locale.forLanguageTag(languageTag)
            reload(locale)
          }
        })
      }
    }
  }
}
