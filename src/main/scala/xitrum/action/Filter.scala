package xitrum.action

import scala.collection.mutable.ArrayBuffer

trait Filter {
  private val beforeFilters = ArrayBuffer[() => Boolean]()

  def beforeFilter(f: () => Boolean) {
    beforeFilters.append(f)
  }

  def skipBeforeFilter(f: () => Boolean) {
    beforeFilters -= f
  }

  /** Called by Dispatcher */
  def callBeforeFilters: Boolean = beforeFilters.forall { (f) => f() }

  //----------------------------------------------------------------------------

  private val afterFilters = ArrayBuffer[() => Any]()

  def afterFilter(f: () => Any) {
    afterFilters.append(f)
  }

  def skipAfterFilter(f: () => Any) {
    afterFilters -= f
  }

  /** Called by Dispatcher */
  def callAfterFilters { afterFilters.foreach(_()) }

  //----------------------------------------------------------------------------

  private val aroundFilters = ArrayBuffer[(() => Any) => Any]()

  def aroundFilter(f: (() => Any) => Any) {
    aroundFilters.append(f)
  }

  def skipAroundFilter(f: (() => Any) => Any) {
    aroundFilters -= f
  }

  /** Called by Dispatcher */
  def callAroundFilters(executeOrPostback: () => Any) {
    val initialWrapper = () => executeOrPostback()
    val bigWrapper = aroundFilters.foldLeft(initialWrapper) { (wrapper, f) =>
      () => f(wrapper)
    }
    bigWrapper()
  }
}
