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
  // Override things that takes time to calculate by reusing them from parent

  override lazy val at = parent.at

  override lazy val requestCookies  = parent.requestCookies
  override lazy val responseCookies = parent.responseCookies
  override lazy val session         = parent.session

  override lazy val remoteIp = parent.remoteIp
  override lazy val isSsl    = parent.isSsl

  override lazy val serverName = parent.serverName
  override lazy val serverPort = parent.serverPort
}
