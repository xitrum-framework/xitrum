package xitrum.action

import scala.collection.mutable.ArrayBuffer

import xitrum.Action

trait Filter {
  this: Action =>

  private val beforeFilters = ArrayBuffer.empty[() => Any]

  /** Adds a before filter. */
  def beforeFilter(f: => Any) {
    beforeFilters.append(f _)
  }

  /**
   * Called by Dispatcher.
   * Calls all before filters until a filter has responded something.
   *
   * @return false if a before filter has responded something and later before
   * filters and the action's "execute" method should not be called 
   */
  def callBeforeFilters(): Boolean = {
    beforeFilters.forall { bf =>
      bf()
      !isDoneResponding && !forwarding
    }
  }

  //----------------------------------------------------------------------------

  /** Adds an after filter. */
  private val afterFilters = ArrayBuffer.empty[() => Any]

  def afterFilter(f: => Any) {
    afterFilters.append(f _)
  }

  /**
   * Called by Dispatcher.
   * Calls all after filters.
   */
  def callAfterFilters() {
    afterFilters.foreach { af => af() }
  }

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
