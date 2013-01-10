package xitrum.sockjs

trait NonWebSocketSessionListener {
  def onOpenOKCreated()
  def onOpenOKWaiting()

  def onOpenNGAnotherConnectionStillOpen()
  def onOpenNGClosed()

  def onMessages()
}
