package xitrum.i18n

import xitrum.Action

trait I18n {
  this: Action =>

  private var po = PoLoader.load("en")

  def setLanguage(language: String) {
    po = PoLoader.load(language)
  }

  def t(singular: String): String = po.t(singular)

  def t(ctx: String, singular: String) = po.t(ctx, singular)

  def t(singular: String, plural: String, n: Long) = po.t(singular, plural, n)

  def t(ctx: String, singular: String, plural: String, n: Long) = po.t(ctx, singular, plural, n)
}
