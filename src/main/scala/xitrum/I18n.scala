package xitrum

import io.netty.handler.codec.http.HttpHeaders.Names
import xitrum.i18n.PoLoader

trait I18n {
  this: Action =>

  private var language = "en"
  private var po       = PoLoader.load("en")

  def getLanguage = language

  def setLanguage(language: String) {
    this.language = language
    po = PoLoader.load(language)
  }

  /** @return List of languages, high priority first */
  def detectBrowserLanguages: Array[String] = {
    val header = request.getHeader(Names.ACCEPT_LANGUAGE)
    if (header == null) return Array()

    val langs = header.split(",")
    val lang_priorityList = langs.map { lang =>
      val lang_priority = lang.split(";")
      if (lang_priority.size == 2) {
        val lang2    = lang_priority(0).trim
        val priority = try { lang_priority(1).trim.toFloat } catch { case _ => 1.0 }
        (lang2, priority)
      } else {
        (lang.trim, 1.0)
      }
    }

    val highFirst = lang_priorityList.sortBy { case (_, priority) => -priority }
    highFirst.map { case (lang, _) => lang }
  }

  def t(singular: String): String = po.t(singular)

  def tc(ctx: String, singular: String) = po.t(ctx, singular)

  def tn(singular: String, plural: String, n: Long) = po.t(singular, plural, n)

  def tcn(ctx: String, singular: String, plural: String, n: Long) = po.t(ctx, singular, plural, n)
}
