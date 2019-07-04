package xitrum.handler.inbound
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

import xitrum.Config
import xitrum.Log
import xitrum.Server

import org.scalatest._
class ProxyProtocolHandlerTest extends FlatSpec with Matchers with BeforeAndAfter with Log {
  private val site = "127.0.0.1"
  behavior of "ProxyProtocolV1"

  before {
    Server.start()
  }

  after {
    Server.stop()
  }

  "Source IP" should "be same with IP in request header" in {
    Config.xitrum.reverseProxy.foreach(r => {
      r.proxyProtocolEnabledOpt.foreach(proxyProtocolEnabled => {
        if (proxyProtocolEnabled) {
          val socket = new Socket(site, 8000)
          val out = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream(), "UTF-8"))
          val in = new BufferedReader(
            new InputStreamReader(socket.getInputStream()))
          out.write("PROXY TCP4 192.168.1.2 192.168.1.3 8080 8000\r\nGET / HTTP/1.1\r\nConnection: close\r\n\r\n")
          out.flush()
          var line:String = null
          val headers = scala.collection.mutable.Map[String, String]()
          while ({line = in.readLine; line != null}) {
            line.split(":").toList match {
              case h:List[String] if h.length > 1 =>
                headers += (h(0)->h(1))
              case _=>
            }
            log.debug(line)
          }
          log.debug(headers.toString())
          out.close()
          in.close()
          headers.getOrElse("X-Forwarded-Fox", "") should  equal("192.168.1.2")
        }
      })

    })
    1 should equal(1)
  }
}
