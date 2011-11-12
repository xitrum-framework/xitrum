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

  private val arroundFilters = ArrayBuffer[(() => Any) => Any]()

  def arroundFilter(f: (() => Any) => Any) {
    arroundFilters.append(f)
  }

  def skipArroundFilter(f: (() => Any) => Any) {
    arroundFilters -= f
  }

  /** Called by Dispatcher */
  def callArroundFilters(execute: () => Any) {
    val initialWrapper = () => execute()
    val bigWrapper = arroundFilters.foldLeft(initialWrapper) { (wrapper, f) =>
      () => f(wrapper)
    }
    bigWrapper()
  }
}
