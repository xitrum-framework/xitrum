package xitrum.handler.inbound

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket

import scala.collection.mutable.{Map => MMap}

import org.scalatest.{BeforeAndAfter, Ignore}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import xitrum.Action
import xitrum.Log
import xitrum.Server
import xitrum.SkipCsrfCheck
import xitrum.annotation.GET

@GET("/test/xff")
class XFFTestAction extends Action with SkipCsrfCheck {
  def execute(): Unit = {
    respondText("X-Forwarded-For: " + request.headers.get("X-Forwarded-For"))
  }
}

@Ignore
class ProxyProtocolHandlerTest extends AnyFlatSpec with Matchers with BeforeAndAfter with Log {
  behavior of "ProxyProtocol"

  private val site = "127.0.0.1"

  before {
    Server.start()
  }

  after {
    Server.stop()
  }

  private def getHeaders(header: String): MMap[String, String] = {
    val socket = new Socket(site, 8000)

    val out = new BufferedWriter(
      new OutputStreamWriter(socket.getOutputStream, "UTF-8")
    )
    val in = new BufferedReader(
      new InputStreamReader(socket.getInputStream)
    )

    out.write(s"PROXY TCP4 192.168.1.2 192.168.1.3 8080 8000\r\nGET /my_site/test/xff HTTP/1.1\r\nConnection: close$header\r\n\r\n")
    out.flush()

    var line: String = null
    val headers = MMap[String, String]()
    while ({line = in.readLine; line != null}) {
      line.split(":").toList match {
        case h: List[String] if h.length > 1 =>
          headers += (h.head -> h(1).trim())
        case _=>
          Log.debug(line)
      }
    }
    out.close()
    in.close()

    headers
  }

  "X-Forwarded-For" should "be same with IP in request message" in {
    getHeaders("")("X-Forwarded-For") should  equal("192.168.1.2")
  }

  "X-Forwarded-For" should "be same with IP in request header" in {
    getHeaders("\r\nX-Forwarded-For: 192.168.1.4")("X-Forwarded-For") should  equal("192.168.1.4, 192.168.1.2")
  }
}
