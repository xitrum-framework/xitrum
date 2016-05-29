package xitrum.params

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.read
import org.scalatest._

import com.m3.curly.scala._

import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}

import xitrum.Action
import xitrum.Server
import xitrum.SkipCsrfCheck
import xitrum.annotation.GET
import xitrum.annotation.POST

case class ParamsPathResponse(path: String)
case class ParamsQueryResponse(key: Int, keys: Seq[Int])
case class ParamsPayloadResponse(key: Int, keys: Seq[Int], locale: String, optional: Option[String])

@GET("/test/params/path/:path")
@POST("/test/params/path/:path")
class ParamsPathTestAction extends Action with SkipCsrfCheck {
  def execute() {
    respondJson(ParamsPathResponse(param("path")))
  }
}

@GET("test/params/query")
@POST("test/params/query")
class ParamsQueryTestAction extends Action with SkipCsrfCheck {
  def execute() {
    val key = param[Int]("key")
    val keys = params[Int]("keys")

    respondJson(ParamsQueryResponse(key, keys))
  }
}

@POST("test/params/payload")
class ParamsPayloadTestAction extends Action with SkipCsrfCheck {
  def execute() {
    val key = param[Int]("key")
    val keys = params[Int]("keys")
    val locale = param[String]("locale")
    val optional = paramo[String]("optional")

    respondJson(ParamsPayloadResponse(key, keys, locale, optional))
  }
}

class ParamsTest extends FlatSpec with Matchers with BeforeAndAfter {
  implicit val formats = Serialization.formats(NoTypeHints)
  val base = "http://127.0.0.1:8000"

  behavior of "param* method"

  before {
    Server.start()
  }

  after {
    Server.stop()
  }

  "[GET] param" should "extract string value from path" in {
    val response = HTTP.get(s"$base/test/params/path/test-value")
    shouldEqual(response, ParamsPathResponse("test-value"))
  }

  "[POST] param" should "extract string value from path" in {
    val response = HTTP.post(s"$base/test/params/path/test-value")
    shouldEqual(response, ParamsPathResponse("test-value"))
  }

  "[GET] param" should "extract int value/s from query" in {
    val response = HTTP.get(s"$base/test/params/query", "key" -> 1, "keys" -> 2, "keys" -> 3)
    shouldEqual(response, ParamsQueryResponse(1, Seq(2, 3)))
  }

  "[POST] param" should "extract int value/s from query" in {
    val response = HTTP.post(s"$base/test/params/query?key=1&keys=2&keys=3")
    shouldEqual(response, ParamsQueryResponse(1, Seq(2, 3)))
  }

  "[POST] param" should "extract values from application/x-www-form-urlencoded payload" in {
    val request = Request(s"$base/test/params/payload")
      .header(
        HttpHeaderNames.CONTENT_TYPE.toString,
        HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString
      )
      .body(
        "key=1&keys=2&keys=3&locale=ru-RU&optional=value".getBytes,
        HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString
      )
    val response = HTTP.post(request)
    shouldEqual(response, ParamsPayloadResponse(1, Seq(2, 3), "ru-RU", Some("value")))
  }

  "[POST] param" should "extract values from application/json payload" in {
    val request = Request(s"$base/test/params/payload")
      .header(HttpHeaderNames.CONTENT_TYPE.toString, "application/json")
      .body("""{"key": 1, "keys": [2, 3], "locale": "ru-RU", "optional": "value"}""".getBytes,
        "application/json")
    val response = HTTP.post(request)
    shouldEqual(response, ParamsPayloadResponse(1, Seq(2, 3), "ru-RU", Some("value")))
  }

  private def shouldEqual[T](response: Response, expected: T)(implicit formats: Formats, mf: Manifest[T]) = {
    if (response.status != 200) {
      println(s"[DEBUG] Response dump:\n${response.textBody}\n------")
    }
    
    response.status should equal(200)
    read[T](response.textBody) shouldEqual expected
  }
}
