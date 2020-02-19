package xitrum.routing

import io.netty.handler.codec.http.HttpMethod

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import xitrum.Action

class RouteTest extends AnyFlatSpec with Matchers {
  behavior of "Route"

  private val route = new Route(HttpMethod.GET, RouteCompiler.compile("/test1/:p1"), classOf[Action], -1)

  "url" should "include Config.baseUrl" in {
    route.url(Map("p1" -> "x", "p2" -> "y")) shouldBe Right("/my_site/test1/x?p2=y")
  }
}
