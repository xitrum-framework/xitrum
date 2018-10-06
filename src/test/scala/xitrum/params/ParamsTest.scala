package xitrum.params

import io.netty.handler.codec.http.{HttpHeaderNames, HttpHeaderValues}
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.Response

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.read
import org.scalatest._

import xitrum.{Action, Log, Server, SkipCsrfCheck}
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

class ParamsTest extends FlatSpec with Matchers with BeforeAndAfter with Log {
  private implicit val formats = Serialization.formats(NoTypeHints)
  private val base = "http://127.0.0.1:8000/my_site"
  private val client = asyncHttpClient()

  behavior of "param* method"

  before {
    Server.start()
  }

  after {
    Server.stop()
  }

  "[GET] param" should "extract string value from path" in {
    val response = client.prepareGet(s"$base/test/params/path/test-value").execute().get()
    shouldEqual(response, ParamsPathResponse("test-value"))
  }

  "[POST] param" should "extract string value from path" in {
    val response = client.preparePost(s"$base/test/params/path/test-value").execute().get()
    shouldEqual(response, ParamsPathResponse("test-value"))
  }

  "[GET] param" should "extract int value/s from query" in {
    val response = client.prepareGet(s"$base/test/params/query")
      .addQueryParam("key", "1")
      .addQueryParam("keys", "2")
      .addQueryParam("keys", "3")
      .execute().get()
    shouldEqual(response, ParamsQueryResponse(1, Seq(2, 3)))
  }

  "[POST] param" should "extract int value/s from query" in {
    val response = client.preparePost(s"$base/test/params/query?key=1&keys=2&keys=3").execute().get()
    shouldEqual(response, ParamsQueryResponse(1, Seq(2, 3)))
  }

  "[POST] param" should "extract values from application/x-www-form-urlencoded payload" in {
    val response = client.preparePost(s"$base/test/params/payload")
      .addHeader(HttpHeaderNames.CONTENT_TYPE.toString, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString)
      .addFormParam("key", "1")
      .addFormParam("keys", "2")
      .addFormParam("keys", "3")
      .addFormParam("locale", "ru-RU")
      .addFormParam("optional", "value")
      .execute().get()
    shouldEqual(response, ParamsPayloadResponse(1, Seq(2, 3), "ru-RU", Some("value")))
  }

  "[POST] param" should "extract values from application/json payload" in {
    val response = client.preparePost(s"$base/test/params/payload")
      .addHeader(HttpHeaderNames.CONTENT_TYPE.toString, "application/json")
      .setBody("""{"key": 1, "keys": [2, 3], "locale": "ru-RU", "optional": "value"}""".getBytes)
      .execute().get()
    shouldEqual(response, ParamsPayloadResponse(1, Seq(2, 3), "ru-RU", Some("value")))
  }

  private def shouldEqual[T](response: Response, expected: T)(implicit formats: Formats, mf: Manifest[T]) = {
    if (response.getStatusCode != 200) {
      log.warn(s"Response:\n$response")
    }
    
    response.getStatusCode should equal(200)
    read[T](response.getResponseBody) shouldEqual expected
  }
}
