package xt

import java.util.Properties
import java.net.URL
import java.io.FileInputStream

object Config {
  private val properties = {
    val url = ClassLoader.getSystemResource("xitrum.properties")
    val ret = new Properties
    ret.load(new FileInputStream(url.getFile()))
    ret
  }

  val httpPort = properties.getProperty("http_port").toInt

  val filterParams = properties.getProperty("filter_params").split(", ")
}
