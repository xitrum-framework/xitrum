package xitrum.action

import scala.collection.mutable.ArrayBuffer

import xitrum.ActionEnv
import xitrum.routing.Route

case class BeforeFilter(body: ()          => Boolean, only: ArrayBuffer[Route], except: ArrayBuffer[Route])
case class AfterFilter (body: ()          => Any,     only: ArrayBuffer[Route], except: ArrayBuffer[Route])
case class AroundFilter(body: (() => Any) => Any,     only: ArrayBuffer[Route], except: ArrayBuffer[Route])

trait Filter {
  this: ActionEnv =>

  private val beforeFilters = ArrayBuffer[BeforeFilter]()

  def beforeFilter(f: => Boolean): () => Boolean = {
    val ret = () => f
    beforeFilters.append(BeforeFilter(ret, ArrayBuffer(), ArrayBuffer()))
    ret
  }

  def beforeFilter(only: Seq[ActionEnv] = Seq.empty, except: Seq[ActionEnv] = Seq.empty)(f: => Boolean): () => Boolean = {
    if (only.nonEmpty && except.nonEmpty) throw new Exception("Can't specify 'both' only and 'except'")

    val ret = () => f
    val onlyBuffer   = ArrayBuffer[Route](); onlyBuffer.appendAll(only.map(_.route))
    val exceptBuffer = ArrayBuffer[Route](); exceptBuffer.appendAll(except.map(_.route))
    beforeFilters.append(BeforeFilter(ret, onlyBuffer, exceptBuffer))
    ret
  }

  def skipBeforeFilter(body: () => Boolean, only: Seq[ActionEnv] = Seq.empty, except: Seq[ActionEnv] = Seq.empty) {
    if (only.nonEmpty && except.nonEmpty) throw new Exception("Can't specify both 'only' and 'except'")

    if (only.isEmpty && except.isEmpty) {
      var i = 0
      beforeFilters.foreach { bf =>
        if (bf.body == body) {
          beforeFilters.remove(i)
          return
        }
        i += 1
      }
    } else if (only.nonEmpty) {
      beforeFilters.foreach { bf =>
        if (bf.body == body) {
          bf.only.clear()
          bf.except.clear()
          bf.except.appendAll(only.map(_.route))
          return
        }
      }
    } else if (except.nonEmpty) {
      beforeFilters.foreach { bf =>
        if (bf.body == body) {
          bf.only.clear()
          bf.except.clear()
          bf.only.appendAll(except.map(_.route))
          return
        }
      }
    }
  }

  /** Called by Dispatcher */
  def callBeforeFilters(): Boolean = beforeFilters.forall { bf =>
    val route = currentAction.route
    if (bf.only.nonEmpty) {
      if (bf.only.contains(route)) bf.body() else true
    } else if (bf.except.nonEmpty) {
      if (bf.except.contains(route)) true else bf.body()
    } else {
      bf.body()
    }
  }

  //----------------------------------------------------------------------------

  private val afterFilters = ArrayBuffer[AfterFilter]()

  def afterFilter(f: => Any): () => Any = {
    val ret = () => f
    afterFilters.append(AfterFilter(ret, ArrayBuffer(), ArrayBuffer()))
    ret
  }

  def afterFilter(only: Seq[ActionEnv] = Seq.empty, except: Seq[ActionEnv] = Seq.empty)(f: => Any): () => Any = {
    if (only.nonEmpty && except.nonEmpty) throw new Exception("Can't specify both 'only' and 'except'")

    val ret = () => f
    val onlyBuffer   = ArrayBuffer[Route](); onlyBuffer.appendAll(only.map(_.route))
    val exceptBuffer = ArrayBuffer[Route](); exceptBuffer.appendAll(except.map(_.route))
    afterFilters.append(AfterFilter(ret, onlyBuffer, exceptBuffer))
    ret
  }

  def skipAfterFilter(body: () => Any, only: Seq[ActionEnv] = Seq.empty, except: Seq[ActionEnv] = Seq.empty) {
    if (only.nonEmpty && except.nonEmpty) throw new Exception("Can't specify both 'only' and 'except'")

    if (only.isEmpty && except.isEmpty) {
      var i = 0
      afterFilters.foreach { af =>
        if (af.body == body) {
          afterFilters.remove(i)
          return
        }
        i += 1
      }
    } else if (only.nonEmpty) {
      afterFilters.foreach { af =>
        if (af.body == body) {
          af.only.clear()
          af.except.clear()
          af.except.appendAll(only.map(_.route))
          return
        }
      }
    } else if (except.nonEmpty) {
      afterFilters.foreach { af =>
        if (af.body == body) {
          af.only.clear()
          af.except.clear()
          af.only.appendAll(except.map(_.route))
          return
        }
      }
    }
  }

  /** Called by Dispatcher */
  def callAfterFilters() {
    val route = currentAction.route
    afterFilters.foreach { af =>
      if (af.only.nonEmpty) {
        if (af.only.contains(route)) af.body()
      } else if (af.except.nonEmpty) {
        if (!af.except.contains(route)) af.body()
      } else {
        af.body()
      }
    }
  }

  //----------------------------------------------------------------------------

  private val aroundFilters = ArrayBuffer[AroundFilter]()

  def aroundFilter(f: (() => Any) => Any): (() => Any) => Any = {
    aroundFilters.append(AroundFilter(f, ArrayBuffer(), ArrayBuffer()))
    f
  }

  def aroundFilter(only: Seq[ActionEnv] = Seq.empty, except: Seq[ActionEnv] = Seq.empty)(f: (() => Any) => Any): (() => Any) => Any = {
    if (only.nonEmpty && except.nonEmpty) throw new Exception("Can't specify both 'only' and 'except'")

    val onlyBuffer   = ArrayBuffer[Route](); onlyBuffer.appendAll(only.map(_.route))
    val exceptBuffer = ArrayBuffer[Route](); exceptBuffer.appendAll(except.map(_.route))
    aroundFilters.append(AroundFilter(f, onlyBuffer, exceptBuffer))
    f
  }

  def skipAroundFilter(body: (() => Any) => Any, only: Seq[ActionEnv] = Seq.empty, except: Seq[ActionEnv] = Seq.empty) {
    if (only.nonEmpty && except.nonEmpty) throw new Exception("Can't specify both 'only' and 'except'")

    if (only.isEmpty && except.isEmpty) {
      var i = 0
      aroundFilters.foreach { af =>
        if (af.body == body) {
          aroundFilters.remove(i)
          return
        }
        i += 1
      }
    } else if (only.nonEmpty) {
      aroundFilters.foreach { af =>
        if (af.body == body) {
          af.only.clear()
          af.except.clear()
          af.except.appendAll(only.map(_.route))
          return
        }
      }
    } else if (except.nonEmpty) {
      aroundFilters.foreach { af =>
        if (af.body == body) {
          af.only.clear()
          af.except.clear()
          af.only.appendAll(except.map(_.route))
          return
        }
      }
    }
  }


  /** Called by Dispatcher */
  def callAroundFilters(action: ActionEnv) {
    val initialWrapper = () => action.body()
    val bigWrapper = aroundFilters.foldLeft(initialWrapper) { (wrapper, af) =>
      () => af.body(wrapper)
    }
    bigWrapper()
  }
}
