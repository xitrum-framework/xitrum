package xitrum

import scala.util.control.NonFatal
import io.netty.handler.codec.http.HttpHeaders
import HttpHeaders.Names

import xitrum.i18n.PoLoader

trait I18n {
  this: Action =>

  // For Component language to work, this should only be used in
  // "language" and "language_=" below. Other places should not use "_language"
  // directly. See ComponentTest.
  private var _language = "en"

  /** Default language is "en". */
  def language = _language

  /** Sets current language. There should be i18n/lang.po file in classpath. */
  def language_=(lang: String) { _language = lang }

  /** @return List of languages sorted by priority from high to low */
  lazy val browserLanguages: Array[String] = {
    val header = HttpHeaders.getHeader(request, Names.ACCEPT_LANGUAGE)
    if (header == null) {
      Array()
    } else {
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
  }

  /** If there's no suitable language, language is still the default "en". */
  def autosetLanguage(resourceLanguages: String*) {
    val lango = browserLanguages.find(resourceLanguages.contains)
    lango.foreach(language_=)
  }

  def t(singular: String) = PoLoader.get(language).t(singular)
  def tc(ctx: String, singular: String) = PoLoader.get(language).tc(ctx, singular)
  def tn(singular: String, plural: String, n: Long) = PoLoader.get(language).tn(singular, plural, n)
  def tcn(ctx: String, singular: String, plural: String, n: Long) = PoLoader.get(language).tcn(ctx, singular, plural, n)

  def tf(singular: String, args: Any*) = t(singular).format(args:_*)
  def tcf(ctx: String, singular: String, args: Any*) = tc(ctx, singular).format(args:_*)
  def tnf(singular: String, plural: String, n: Long, args: Any*) = tn(singular, plural, n).format(args:_*)
  def tcnf(ctx: String, singular: String, plural: String, n: Long, args: Any*) = tcn(ctx, singular, plural, n).format(args:_*)
}
