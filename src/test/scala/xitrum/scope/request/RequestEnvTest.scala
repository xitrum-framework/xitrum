package xitrum.scope.request

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import xitrum.Action

class RequestEnvTest extends FlatSpec with Matchers {

  behavior of "RequestEnv"

  it should "should have atjs method" in {
    val dummy = new Action() { def execute() {} }
    dummy.at("test_string") = "abc"
    dummy.atjs("test_string") should equal ("\"abc\"")
  }

}