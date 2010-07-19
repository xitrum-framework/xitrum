package colinh

import xt.server.Server

object Http {
  def main(args: Array[String]) {
    val s = new Server
    s.start
  }
}
