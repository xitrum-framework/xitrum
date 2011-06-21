package xitrum.handler

import xitrum.Config

object BaseUri {
  def remove(originalUri: String): Option[String] = {
    if (originalUri == Config.baseUri)
      Some("/")
    else if (originalUri.startsWith(Config.baseUri + "/"))
      Some(originalUri.substring(Config.baseUri.length))
    else
      None
  }
}
