package xt

import java.util.Properties
import java.net.URL
import java.io.FileInputStream

object Config {
  private val properties = {
    val url = ClassLoader.getSystemResource("xitrum.properties")
    val ret = new Properties
    if (url != null) ret.load(new FileInputStream(url.getFile()))
    ret
  }

  val isProductionMode = (System.getProperty("xitrum.mode") == "production")

  val httpPort = properties.getProperty("http_port", "8080").toInt

  val filterParams = properties.getProperty("filter_params", "password").split(", ")
}
