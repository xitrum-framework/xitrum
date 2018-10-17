package xitrum.action

import org.asynchttpclient.Dsl.asyncHttpClient

import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.read
import org.scalatest._

import xitrum.{Action, Log, Server, SkipCsrfCheck, version}
import xitrum.annotation.GET

case class TestUrls(abs: String, rel: String, webJars: String)

@GET("/test")
class UrlTestAction extends Action with SkipCsrfCheck {
  def execute() {
    respondJson(TestUrls(absUrl[UrlTestAction], url[UrlTestAction], webJarsUrl("xitrum/" + version + "/ajax.gif")))
  }
}

class UrlTest extends FlatSpec with Matchers with BeforeAndAfter with Log {
  private val site = "http://127.0.0.1:8000"
  private val base = "/my_site"
  private val path = "/test"

  private val absPath = s"$site$base$path"
  private val relPath = s"$base$path"

  private val client = asyncHttpClient()
  private implicit val formats = Serialization.formats(NoTypeHints)

  behavior of "Url"

  before {
    Server.start()
  }

  after {
    Server.stop()
  }

  "url and absUrl" should "not include and include baseUrl" in {
    val response = client.prepareGet(absPath).execute().get()

    response.getStatusCode should equal(200)

    val testUrls = read[TestUrls](response.getResponseBody)
    testUrls.abs shouldEqual absPath
    testUrls.rel shouldEqual relPath

    client.prepareGet(site + testUrls.webJars).execute().get().getStatusCode should equal(200)
  }
}
