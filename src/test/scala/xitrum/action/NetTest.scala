package xitrum.action

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import xitrum.Log

class NetTest extends AnyFlatSpec with Matchers with Log {
  behavior of "proxyNotAllowed"

  "0.0.0.0" should "allowed" in {
    Net.proxyNotAllowed("0.0.0.0") should equal(true)
  }

  "127.0.0.1" should "not allowed" in {
    Net.proxyNotAllowed("127.0.0.1") should equal(false)
  }

  "Ips in range 10.0.0.0/24" should " not allowed" in {
    Net.proxyNotAllowed("10.0.0.11") should equal(false)
  }
}
