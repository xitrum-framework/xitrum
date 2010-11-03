package xt.framework

import scala.collection.mutable.{Map => MMap}

trait ControllerFilter {
	val beforeFilters = MMap[String, () => Boolean]()

	def beforeFilter(name: String)(f: () => Boolean) {
		beforeFilters(name) = f
	}

	def skipBeforeFilter(name: String) {
		beforeFilters.remove(name)
	}
}
