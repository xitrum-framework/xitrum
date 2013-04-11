package xitrum

trait SockJsActor extends ActionActor {
  case class SockJsText(text: String)
}
