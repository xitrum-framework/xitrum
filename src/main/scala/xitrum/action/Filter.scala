package xitrum.action

import scala.collection.mutable.{Map => MMap}

trait Filter {
  val beforeFilters = MMap[String, () => Boolean]()

  /** Called by Dispatcher */
  def callBeforeFilters: Boolean =
    beforeFilters.forall { case (name, function) => function() }
}
