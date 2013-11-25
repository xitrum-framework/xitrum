package xitrum.scope.request

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import xitrum.Action

class RequestEnvTest extends FlatSpec with Matchers {
  behavior of "RequestEnv"

  it should "should have atJs method" in {
    val action = new Action() { def execute() {} }

    action.at("test_string") = "abc"
    action.atJs("test_string") should equal("\"abc\"")

    action.at("test_array") = Seq(1, 2, 3)
    action.atJs("test_array") should equal("[1,2,3]")
  }
}
