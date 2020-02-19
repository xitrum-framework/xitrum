package xitrum

import io.netty.handler.codec.http.cookie.Cookie
import xitrum.scope.request.At

import java.util.{List => JList, Locale}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait Component extends Action {
  private var parent: Action = _

  def apply(action: Action): Unit = {
    parent = action
    this.handlerEnv = action.handlerEnv
  }

  def execute(): Unit = {}

  override def jsAddToView(js: Any): Unit = { parent.jsAddToView(js) }

  //----------------------------------------------------------------------------
  // Things that must be in sync with parent

  override lazy val at: At = parent.at

  override def language: String = parent.language
  override def locale: Locale = parent.locale
  override def language_=(language: String): Unit = { parent.language = language }

  //----------------------------------------------------------------------------
  // Reuse things from parent, things that can be calculated from handlerEnv,
  // but takes time to calculate

  override lazy val requestCookies: collection.Map[String, String] = parent.requestCookies
  override lazy val responseCookies: ArrayBuffer[Cookie] = parent.responseCookies
  override lazy val session: mutable.Map[String, Any] = parent.session

  override lazy val remoteIp: String = parent.remoteIp
  override lazy val isSsl: Boolean = parent.isSsl

  override lazy val serverName: String = parent.serverName
  override lazy val serverPort: Int = parent.serverPort

  override lazy val browserLanguages: JList[Locale.LanguageRange] = parent.browserLanguages
}
