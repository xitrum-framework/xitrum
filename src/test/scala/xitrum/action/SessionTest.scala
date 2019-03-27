package xitrum.action

import org.asynchttpclient.Dsl.asyncHttpClient
import org.scalatest._
import xitrum.annotation.GET
import xitrum.scope.session.TransientSession
import xitrum.{Action, Log, Server}

import scala.collection.JavaConverters._

@GET("/session")
class SessionAccessAction extends Action {
  def execute(): Unit = {
    if (param[Boolean]("access")) {
      session.put("Hello", "World")
    }
    respond()
  }
}

@GET("/sessionless")
class SessionlessAction extends Action with TransientSession {
  def execute(): Unit = {
    if (param[Boolean]("access")) {
      session.put("Hello", "World")
    }
    respond()
  }
}

// sessions are retrieved and stored only if the map is accessed (see SessionEnv)
class SessionTest extends FlatSpec with BeforeAndAfter with Log {

  private val basePath = "http://127.0.0.1:8000/my_site"
  private val client = asyncHttpClient()

  behavior of "Session"

  before {
    Server.start()
  }

  after {
    Server.stop()
  }

  "Accessing session" should "provide a session if not transient" in {
    val response = client.prepareGet(basePath + "/session?access=true").execute().get()
    assert(response.getCookies.asScala.exists(_.name == "_session"))
  }

  it should "not provide a session if transient" in {
    val response = client.prepareGet(basePath + "/sessionless?access=true").execute().get()
    assert(!response.getCookies.asScala.exists(_.name == "_session"))
  }

  "Not accessing session" should "provide no session if not transient" in {
    val response = client.prepareGet(basePath + "/session?access=false").execute().get()
    assert(!response.getCookies.asScala.exists(_.name == "_session"))
  }

  it should "provide no session if transient" in {
    val response = client.prepareGet(basePath + "/sessionless?access=false").execute().get()
    assert(!response.getCookies.asScala.exists(_.name == "_session"))
  }
}
