package my_package

import xitrum.server.Server

object BootClass {
  def main(args: Array[String]) {
    val server = new Server
    server.start
  }
}
