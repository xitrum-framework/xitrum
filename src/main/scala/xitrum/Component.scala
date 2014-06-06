package xitrum

trait Component extends Action {
  private var parent: Action = _

  def apply(action: Action) {
    parent = action
    this.handlerEnv = action.handlerEnv
  }

  def execute() {}

  override def jsAddToView(js: Any) { parent.jsAddToView(js) }

  //----------------------------------------------------------------------------
  // Things that must be in sync with parent

  override lazy val at = parent.at

  override def language = parent.language
  override def language_=(lang: String) { parent.language = lang }

  //----------------------------------------------------------------------------
  // Reuse things from parent, things that can be calculated from handlerEnv,
  // but takes time to calculate

  override lazy val requestCookies  = parent.requestCookies
  override lazy val responseCookies = parent.responseCookies
  override lazy val session         = parent.session

  override lazy val remoteIp = parent.remoteIp
  override lazy val isSsl    = parent.isSsl

  override lazy val serverName = parent.serverName
  override lazy val serverPort = parent.serverPort

  override lazy val browserLanguages = parent.browserLanguages
}
