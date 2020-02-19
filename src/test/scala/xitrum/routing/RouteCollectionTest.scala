package xitrum.routing

import scala.collection.mutable.{Map => MMap}

import io.netty.handler.codec.http.{HttpMethod, QueryStringDecoder}
import io.netty.util.CharsetUtil

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import xitrum.Action
import xitrum.scope.request.PathInfo

import scala.collection.mutable.ArrayBuffer

class RouteCollectionTest extends AnyFlatSpec with Matchers {
  behavior of "RouteCollection"

  private val route = new Route(HttpMethod.GET, RouteCompiler.compile("/test1/:p1"), classOf[Action], -1)

  private val routeCollection = new RouteCollection(
    ArrayBuffer(route), ArrayBuffer.empty, ArrayBuffer.empty,
    ArrayBuffer.empty, ArrayBuffer.empty, ArrayBuffer.empty,
    ArrayBuffer.empty, ArrayBuffer.empty, ArrayBuffer.empty,
    ArrayBuffer.empty, ArrayBuffer.empty, ArrayBuffer.empty,
    ArrayBuffer.empty, ArrayBuffer.empty, ArrayBuffer.empty,
    ArrayBuffer.empty, ArrayBuffer.empty, ArrayBuffer.empty,
    new SockJsRouteMap(MMap.empty), Map.empty,
    None, None
  )

  private def testMatchRoute(uri: String): Unit = {
    uri should "match /test1/:p1" in {
      val decoder = new QueryStringDecoder(uri, CharsetUtil.UTF_8)
      val opt = routeCollection.route(HttpMethod.GET, new PathInfo(decoder, CharsetUtil.UTF_8))

      opt.isDefined shouldBe true
      opt.get._1 shouldBe route
    }
  }

  private def testNotMatchRoute(uri: String): Unit = {
    uri should "match /test1/:p1" in {
      val decoder = new QueryStringDecoder(uri, CharsetUtil.UTF_8)
      val opt = routeCollection.route(HttpMethod.GET, new PathInfo(decoder, CharsetUtil.UTF_8))

      opt.isDefined shouldBe false
    }
  }

  testMatchRoute("/test1/123")
  testMatchRoute("/test1/123?x=1&y=2")

  testMatchRoute("/test1/123%2F456")
  testMatchRoute("/test1/123%2F456?x=1&y=2")

  testNotMatchRoute("/test1/123/456")
  testNotMatchRoute("/test1/123/456?x=1&y=2")
}

