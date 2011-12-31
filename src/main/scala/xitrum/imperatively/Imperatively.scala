package xitrum.imperatively

import scala.xml.NodeBuffer

trait Imperatively extends GenericImperatively[Map[String, String], NodeBuffer] with Serializable {
  lazy val stepName = "step-" + getClass.getName

  def nextStep: Step = SessionHolder.session.get(stepName) match {
    case None    => ((x: Map[String, String]) => imperatively)
    case Some(s) => s.asInstanceOf[Step]
  }

  def nextStep_=(s: Step) = {
    SessionHolder.session(stepName) = s
  }
}
