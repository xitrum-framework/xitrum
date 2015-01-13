package xitrum

import scala.util.control.NonFatal

/**
 * This is the interface for cache implementations of Xitrum. All methods do not
 * take callbacks, because cache should be fast. The point of using cache is
 * to become faster. There's no point in using a slow cache.
 */
abstract class Cache {
  /**
   * Cache engine like Hazelcast may take serveral seconds to start, this method
   * is called at Xitrum server start to force the cache to start, instead of
   * letting it start lazily at first cache access.
   */
  def start()

  def stop()

  def isDefinedAt(key: Any): Boolean

  def get(key: Any): Option[Any]

  def put(key: Any, value: Any)

  def putIfAbsent(key: Any, value: Any)

  def putSecond(key: Any, value: Any, seconds: Int)

  def putSecondIfAbsent(key: Any, value: Any, seconds: Int)

  def remove(key: Any)

  def clear()

  //----------------------------------------------------------------------------

  def getAs[T](key: Any): Option[T] = get(key).asInstanceOf[Option[T]]

  def putMinute(key: Any, value: Any, minutes: Int) { putSecond(key, value, minutes * 60) }
  def putHour  (key: Any, value: Any, hours:   Int) { putSecond(key, value, hours   * 60 * 60) }
  def putDay   (key: Any, value: Any, days:    Int) { putSecond(key, value, days    * 60 * 60 * 24) }

  def putMinuteIfAbsent(key: Any, value: Any, minutes: Int) { putSecondIfAbsent(key, value, minutes * 60) }
  def putHourIfAbsent  (key: Any, value: Any, hours:   Int) { putSecondIfAbsent(key, value, hours   * 60 * 60) }
  def putDayIfAbsent   (key: Any, value: Any, days:    Int) { putSecondIfAbsent(key, value, days    * 60 * 60 * 24) }
}

object Cache {
  /**
   * Cache config in xitrum.conf can be in 2 forms:
   *
   * {{{
   * cache = my.Cache
   * }}}
   *
   * Or if the cache needs additional options:
   *
   * {{{
   * cache {
   *   "my.Cache" {
   *     option1 = value1
   *     option2 = value2
   *   }
   *
   *   # - Commented out:   Cache is automatically disabled in development mode,
   *   #                    and enabled in production mode.
   *   # - enabled = true:  Force cache to be enabled even in development mode.
   *   # - enabled = false: Force cache to be disabled even in production mode.
   *   enabled = true
   * }
   * }}}
   */
  def loadFromConfig(): Cache = {
    try {
      val enabled =
        if (Config.xitrum.config.hasPath("cache.enabled"))
          Config.xitrum.config.getBoolean("cache.enabled")
        else
          Config.productionMode

      val cache =
        if (enabled) {
          // DualConfig works with config with only one entry
          val withoutEnabled = Config.xitrum.config.withoutPath("cache.enabled")
          DualConfig.getClassInstance[Cache](withoutEnabled, "cache")
        } else {
          dummy()
        }

      cache.start()
      cache
    } catch {
      case NonFatal(e) =>
        Config.exitOnStartupError("Could not load cache engine, please check config/xitrum.conf", e)
        throw e
    }
  }

  private def dummy(): Cache = new Cache {
    def start() {}

    def stop() {}

    def isDefinedAt(key: Any) = false

    def get(key: Any) = None

    def put(key: Any, value: Any) {}

    def putIfAbsent(key: Any, value: Any) {}

    def putSecond(key: Any, value: Any, seconds: Int) {}

    def putSecondIfAbsent(key: Any, value: Any, seconds: Int) {}

    def remove(key: Any) {}

    def clear() {}
  }
}
