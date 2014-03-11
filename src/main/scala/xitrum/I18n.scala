package xitrum

import scala.util.control.NonFatal
import io.netty.handler.codec.http.HttpHeaders
import HttpHeaders.Names

import xitrum.i18n.PoLoader

trait I18n {
  this: Action =>

  /** Default language is "en". */
  var language = "en"

  /** @return List of languages sorted by priority from high to low */
  def browserLanguages: Array[String] = {
    val header = HttpHeaders.getHeader(request, Names.ACCEPT_LANGUAGE)
    if (header == null) return Array()

    val langs = header.split(",")
    val lang_priorityList = langs.map { lang =>
      val lang_priority = lang.split(";")
      if (lang_priority.size == 2) {
        val lang2    = lang_priority(0).trim
        val priority = try { lang_priority(1).trim.toFloat } catch { case NonFatal(e) => 1.0 }
        (lang2, priority)
      } else {
        (lang.trim, 1.0)
      }
    }

    val highFirst = lang_priorityList.sortBy { case (_, priority) => -priority }
    highFirst.map { case (lang, _) => lang }
  }

  /** If there's no suitable language, language is still the default "en". */
  def autosetLanguage(resourceLanguages: String*) {
    for (lang <- browserLanguages) {
      if (resourceLanguages.contains(lang)) {
        language = lang
        return
      }
    }
  }

  def t(singular: String) = PoLoader.get(language).t(singular)
  def tc(ctx: String, singular: String) = PoLoader.get(language).t(ctx, singular)
  def tn(singular: String, plural: String, n: Long) = PoLoader.get(language).t(singular, plural, n)
  def tcn(ctx: String, singular: String, plural: String, n: Long) = PoLoader.get(language).t(ctx, singular, plural, n)

  def tf(singular: String, args: Any*) = t(singular).format(args:_*)
  def tcf(ctx: String, singular: String, args: Any*) = tc(ctx, singular).format(args:_*)
  def tnf(singular: String, plural: String, n: Long, args: Any*) = tn(singular, plural, n).format(args:_*)
  def tcnf(ctx: String, singular: String, plural: String, n: Long, args: Any*) = tcn(ctx, singular, plural, n).format(args:_*)
}
