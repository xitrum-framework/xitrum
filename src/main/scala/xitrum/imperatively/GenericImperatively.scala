package xitrum.imperatively

import scala.util.continuations.{cps, reset, shift}

/**
 * See:
 *   http://www.earldouglas.com/continuation-based-web-workflows-part-two/
 *   http://stackoverflow.com/questions/6062003/event-listeners-with-scala-continuations
 *   http://jim-mcbeath.blogspot.com/2010/08/delimited-continuations.html
 */
trait GenericImperatively[A, B] {
  type Step = A => B

  type imp = cps[B]

  def nextStep: Step
  def nextStep_=(next: Step): Unit

  def prompt(b: B): A @imp =
    shift { k: (A => B) =>
      nextStep = k
      b
    }

  def imperatively: B = reset {
    val resp: B = workflow()
    nextStep = (x: A) => imperatively
    resp
  }

  def workflow(): B @imp
}
