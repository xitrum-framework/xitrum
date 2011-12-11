package xitrum.action

import xitrum.Action

trait WebSocket {
  this: Action =>

  def onOpen {}

  def onClose {}

  def onMessage(text: String) {}

  def onMessage(bytes: Array[Byte]) {}
}
