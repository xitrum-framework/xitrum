package xitrum.handler

import xitrum.Config

object BaseUri {
  /**
   * Removes the base URI (see config/xitrum.properties) from the original request URL.
   *
   * @return None if the original URL does not start with the base URI
   */
  def remove(originalUri: String): Option[String] = {
    if (originalUri == Config.baseUri)
      Some("/")
    else if (originalUri.startsWith(Config.baseUri + "/"))
      Some(originalUri.substring(Config.baseUri.length))
    else
      None
  }
}
