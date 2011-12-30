package xitrum.imperatively

import scala.util.continuations.{cps, reset, shift}

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
