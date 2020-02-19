package xitrum.local

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LruCacheTest extends AnyFlatSpec with Matchers {
  behavior of "LruCache"

  it should "put/get any object" in {
    System.setProperty("xitrum.mode", "production")
    assert(xitrum.Config.productionMode)

    val cache = new LruCache

    cache.put("forever", "forever")
    cache.get("forever") should equal (Some("forever"))

    cache.putSecond("second", "second", 10)
    cache.get("second") should equal (Some("second"))

    cache.putMinute("minute", "minute", 10)
    cache.get("minute") should equal (Some("minute"))

    cache.putHour("hour", "hour", 10)
    cache.get("hour") should equal (Some("hour"))

    cache.putDay("day", "day", 10)
    cache.get("day") should equal (Some("day"))
  }

  it should "update cache when cache is absent" in {
    System.setProperty("xitrum.mode", "production")
    val cache = new LruCache

    cache.put("forever", "forever")
    cache.get("forever") should equal (Some("forever"))

    cache.putIfAbsent("forever", "forever-new")
    cache.putIfAbsent("forever-new", "forever-new")

    cache.get("forever") should equal (Some("forever"))
    cache.get("forever-new") should equal (Some("forever-new"))
  }

  it should "able to clear cache" in  {
    System.setProperty("xitrum.mode", "production")
    val cache = new LruCache

    cache.put("forever", "forever")
    cache.putSecond("second", "second", 10)
    cache.putMinute("minute", "minute", 10)
    cache.putHour("hour", "hour", 10)
    cache.putDay("day", "day", 10)

    cache.clear()

    cache.get("forever") should equal (None)
    cache.get("second") should equal (None)
    cache.get("minute") should equal (None)
    cache.get("hour") should equal (None)
    cache.get("day") should equal (None)
  }

  it should "able to remove from cache" in  {
    System.setProperty("xitrum.mode", "production")
    val cache = new LruCache

    cache.put("forever", "forever")
    cache.get("forever") should equal (Some("forever"))

    cache.remove("forever")
    cache.get("forever") should equal (None)
  }

  it should "put in cache for 1 second" in {
    System.setProperty("xitrum.mode", "production")
    val cache = new LruCache

    cache.putSecond("second", "second", 1)
    cache.get("second") should equal (Some("second"))

    try {
      // Should not sleep 1500ms because the cache TTL precision is second,
      // not millisecond
      Thread.sleep(2000)
    } catch {
      case _: InterruptedException => fail()
    }

    cache.get("second") should equal (None)
  }

  it should "putIfAbsent can overwrite stale cache" in {
    System.setProperty("xitrum.mode", "production")
    val cache = new LruCache

    cache.putSecond("key", "abc", 1)
    cache.get("key") should equal (Some("abc"))

    try {
      // Should not sleep 1500ms because the cache TTL precision is second,
      // not millisecond
      Thread.sleep(2000)
    } catch {
      case _: InterruptedException => fail()
    }

    cache.putIfAbsent("key", "xyz")
    cache.get("key") should equal (Some("xyz"))
  }
}
