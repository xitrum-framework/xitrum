package xitrum.action

import scala.collection.mutable.{HashMap => MHashMap}

trait Filter {
  val beforeFilters = new MHashMap[String, () => Boolean]()

  /** Called by Dispatcher */
  def callBeforeFilters: Boolean =
    beforeFilters.forall { case (name, function) => function() }
}
