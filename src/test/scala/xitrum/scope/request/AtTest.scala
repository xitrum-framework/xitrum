package xitrum.scope.request

import org.scalatest.{FlatSpec, Matchers}
import xitrum.Action

private class AtTestClass(val a: String, val b: Double)
private case class AtTestCaseClass(a: String, b: Int)

class AtTest extends FlatSpec with Matchers {
  behavior of "At"

  it should "store objects" in {
    val at = new At

    at("int") = 5
    at("string") = "string"

    at("int").asInstanceOf[Int] should equal(5)
    at("string").asInstanceOf[String] should equal("string")

    at[Int]("int") should equal(5)
    at[String]("string") should equal("string")
  }

  it should "throw exception when key is not in at" in {
    val at = new At
    intercept[NoSuchElementException] {
      at("not a key")
    }
  }

  it should "convert plain objects to json" in {
    val at = new At

    at("string") = "string"
    at("int") = 10
    at("class") = new AtTestClass("a_field", 10.5)
    at("caseclass") = new AtTestCaseClass("a_field", 10)

    at.toJson("string") should equal("\"string\"")
    at.toJson("int") should equal("""10""")
    at.toJson("class") should equal("""{"a":"a_field","b":10.5}""")
    at.toJson("caseclass") should equal("""{"a":"a_field","b":10}""")
  }

  it should "render json4s JValue to json" in {
    import org.json4s.JsonDSL._

    val at = new At
    at("jobject") = ("a" -> 1) ~ ("b" -> 2) ~ ("c", 3)
    at.toJson("jobject") should equal("""{"a":1,"b":2,"c":3}""")
  }
}
