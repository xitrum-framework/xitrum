package xitrum.imperatively

import scala.xml.NodeBuffer
import scala.util.continuations.cps
import scala.util.continuations.shift
import scala.util.continuations.reset

trait Imperatively[A,B] {

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

import xitrum.scope.session.Session

object SessionHolder extends java.lang.ThreadLocal[Session] {
  def session: Session = get
}

trait ImperativelyXitrum extends Imperatively[Map[String, String], NodeBuffer] with Serializable {

  def stepName = "step-" + getClass.getName

  def nextStep: Step = SessionHolder.session.get(stepName) match {
    case None    => ((x: Map[String, String]) => imperatively)
    case Some(s) => s.asInstanceOf[Step]
  }

  def nextStep_=(s: Step) = {
    SessionHolder.session(stepName) = s
  }
}

