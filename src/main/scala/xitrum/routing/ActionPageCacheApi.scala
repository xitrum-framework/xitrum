package xitrum.routing

/** APIs for app developer to manipulate action and page cache config programatically */
trait ActionPageCacheApi {
  import Routes._

/*
  def cacheActionSecond(seconds: Int, action: Action) {
    cacheSecs(action) = -seconds
  }

  def cacheActionMinute(minutes: Int, action: Action) {
    cacheActionSecond(minutes * 60, action)
  }

  def cacheActionHour(hours: Int, action: Action) {
    cacheActionMinute(hours * 60, routeMethod)
  }

  def cacheActionDay(days: Int, action: Action) {
    cacheActionHour(days * 24, routeMethod)
  }

  def cachePageSecond(seconds: Int, action: Action) {
    cacheSecs(routeMethod) = seconds
  }

  def cachePageMinute(minutes: Int, action: Action) {
    cachePageSecond(minutes * 60, routeMethod)
  }

  def cachePageHour(hours: Int, action: Action) {
    cachePageMinute(hours * 60, action)
  }

  def cachePageDay(days: Int, action: Action) {
    cachePageHour(days * 24, action)
  }
*/
}
