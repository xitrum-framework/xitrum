package xitrum

import java.util.{Collections, List => JList}
import java.util.Locale

import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.util.Try

import io.netty.handler.codec.http.HttpHeaderNames

import xitrum.i18n.PoLoader

trait I18n {
  this: Action =>

  // For Component language to work, this should only be used in
  // "language" and "language_=" methods below.
  // Other places should not use "_language" directly.
  // See ComponentTest.
  private[this] var _language = "en"

  private[this] var _locale = Locale.ENGLISH

  /** Default language is "en". */
  def language = _language

  /** The locale corresponding to the [[language]]. It will be updated automatically when you update [[language]]. */
  def locale = _locale

  /**
   * Sets current language manually. The language should have a corresponding file
   * `i18n/language.po` in classpath (the language should be in IETF BCP 47 format).
   */
  def language_=(language: String) {
    _language = language
    _locale   = Locale.forLanguageTag(language)
  }

  /** @return List of [[Locale.LanguageRange]] sorted by priority from high to low */
  lazy val browserLanguages: JList[Locale.LanguageRange] = {
    val header = request.headers.get(HttpHeaderNames.ACCEPT_LANGUAGE)
    if (header == null) {
      Collections.emptyList[Locale.LanguageRange]
    } else {
      try {
        Locale.LanguageRange.parse(header)
      } catch {
        case NonFatal(e) =>
          log.debug("Cannot parse Accept-Language header", e)
          Collections.emptyList[Locale.LanguageRange]
      }
    }
  }

  /**
   * Sets current language automatically based on the matching of the "Accept-Language"
   * header against the list of given languages. The languages should have corresponding files
   * `i18n/language.po` in classpath (the language should be in IETF BCP 47 format).
   *
   * If there's no match, the language is still the default "en".
   */
  def autosetLanguage(availableLanguages: String*) {
    // lookupTag may throw exception:
    // https://bugs.openjdk.java.net/browse/JDK-8135061
    val bestMatched = Try(Locale.lookupTag(browserLanguages, availableLanguages.asJava)).getOrElse(null)
    if (bestMatched != null) language = bestMatched
  }

  //----------------------------------------------------------------------------
  // For Component language to work (see ComponentTest),
  // must use "language" and "locale",
  // do not use "_language" and "_locale" directly

  def t(singular: String) =
    PoLoader.get(language).t(singular)

  def tc(ctx: String, singular: String) =
    PoLoader.get(language).tc(ctx, singular)

  def tn(singular: String, plural: String, n: Long) =
    PoLoader.get(language).tn(singular, plural, n)

  def tcn(ctx: String, singular: String, plural: String, n: Long) =
    PoLoader.get(language).tcn(ctx, singular, plural, n)

  /** `formatLocal` using the current locale. */
  def t(singular: String, args: Any*) =
    PoLoader.get(language).t(singular).formatLocal(locale, args:_*)

  /** `formatLocal` using the current locale. */
  def tc(ctx: String, singular: String, args: Any*) =
    PoLoader.get(language).tc(ctx, singular).formatLocal(locale, args:_*)

  /** `formatLocal` using the current locale. */
  def tn(singular: String, plural: String, n: Long, args: Any*) =
    PoLoader.get(language).tn(singular, plural, n).formatLocal(locale, args:_*)

  /** `formatLocal` using the current locale. */
  def tcn(ctx: String, singular: String, plural: String, n: Long, args: Any*) =
    PoLoader.get(language).tcn(ctx, singular, plural, n).formatLocal(locale, args:_*)
}
