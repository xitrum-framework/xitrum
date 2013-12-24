package xitrum.action

import scala.collection.mutable.ArrayBuffer

import xitrum.Action
import xitrum.routing.Route

trait Filter {
  this: Action =>

  private val beforeFilters = ArrayBuffer.empty[() => Boolean]

  def beforeFilter(f: => Boolean) {
    beforeFilters.append(() => f)
  }

  /** Called by Dispatcher */
  def callBeforeFilters(): Boolean = beforeFilters.forall { bf => bf() }

  //----------------------------------------------------------------------------

  private val afterFilters = ArrayBuffer.empty[() => Any]

  def afterFilter(f: => Any) {
    afterFilters.append(() => f)
  }

  /** Called by Dispatcher */
  def callAfterFilters() { afterFilters.foreach { af => af() } }

  //----------------------------------------------------------------------------

  private val aroundFilters = ArrayBuffer.empty[(() => Any) => Any]

  def aroundFilter(f: (() => Any) => Any) {
    aroundFilters.append(f)
  }

  /** Called by Dispatcher */
  def callExecuteWrappedInAroundFilters() {
    val initialWrapper = () => execute()
    val bigWrapper = aroundFilters.foldLeft(initialWrapper) { (wrapper, af) =>
      () => af(wrapper)
    }
    bigWrapper()
  }
}
