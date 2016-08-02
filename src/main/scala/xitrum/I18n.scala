package xitrum

import java.util.{Collections, List => JList}
import java.util.Locale

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import io.netty.handler.codec.http.HttpHeaderNames

import xitrum.i18n.PoLoader

trait I18n {
  this: Action =>

  // For Component locale to work, this should only be used in
  // "locale" and "locale_=" methods below.
  // Other places should not use "_locale" directly. See ComponentTest.
  private var _locale = Locale.ENGLISH

  /** Default locale is "en". */
  def locale = _locale

  /**
   * Sets current locale manually. The locale should have a corresponding file
   * `i18n/languageTag.po` in classpath (the languageTag should be in IETF BCP 47 format).
   */
  def locale_=(locale: Locale) {
    _locale = locale
  }

  /** @return List of [[Locale.LanguageRange]] sorted by priority from high to low */
  lazy val browserLanguageRanges: JList[Locale.LanguageRange] = {
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
   * Sets current locale automatically based on the matching of the "Accept-Language"
   * header against the list of given locales. The locales should have corresponding files
   * `i18n/languageTag.po` in classpath (the languageTag should be in IETF BCP 47 format).
   *
   * If there's no match, the locale is the default "en".
   */
  def autosetLanguage(locales: Locale*) {
    val bestMatched = Locale.lookup(browserLanguageRanges, locales.asJava)
    if (bestMatched != null) locale = bestMatched
  }

  //----------------------------------------------------------------------------

  def t(singular: String) =
    PoLoader.get(locale).t(singular)

  def tc(ctx: String, singular: String) =
    PoLoader.get(locale).tc(ctx, singular)

  def tn(singular: String, plural: String, n: Long) =
    PoLoader.get(locale).tn(singular, plural, n)

  def tcn(ctx: String, singular: String, plural: String, n: Long) =
    PoLoader.get(locale).tcn(ctx, singular, plural, n)

  //----------------------------------------------------------------------------

  /** `formatLocal` using the current locale. */
  def t(singular: String, args: Any*) =
    PoLoader.get(locale).t(singular).formatLocal(locale, args:_*)

  /** `formatLocal` using the current locale. */
  def tc(ctx: String, singular: String, args: Any*) =
    PoLoader.get(locale).tc(ctx, singular).formatLocal(locale, args:_*)

  /** `formatLocal` using the current locale. */
  def tn(singular: String, plural: String, n: Long, args: Any*) =
    PoLoader.get(locale).tn(singular, plural, n).formatLocal(locale, args:_*)

  /** `formatLocal` using the current locale. */
  def tcn(ctx: String, singular: String, plural: String, n: Long, args: Any*) =
    PoLoader.get(locale).tcn(ctx, singular, plural, n).formatLocal(locale, args:_*)
}
