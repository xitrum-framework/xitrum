package xitrum

import scala.collection.mutable.{Map => MMap}
import org.scalatest.{FlatSpec, Matchers}

class OptVarRecoveryTest extends FlatSpec with Matchers {
  behavior of "OptVarRecovery"

  implicit val action = new Action { def execute() {} }

  def newOptVar() = new OptVar[String] {
    private val all = MMap[String, Any](key -> Seq("blah"))
    def getAll(implicit action: Action) = all
  }

  "get" should "throw ClassCastException on first call, then NoSuchElementException on next calls" in {
    val v = newOptVar()
    intercept[ClassCastException]     { v.get }
    intercept[NoSuchElementException] { v.get }
    intercept[NoSuchElementException] { v.get }
  }

  "remove" should "throw ClassCastException on first call, then return None on next calls" in {
    val v = newOptVar()
    intercept[ClassCastException] { v.remove() }
    v.remove() shouldBe None
    v.remove() shouldBe None
  }

  "toOption" should "throw ClassCastException on first call, then return None on next calls" in {
    val v = newOptVar()
    intercept[ClassCastException] { v.toOption }
    v.toOption shouldBe None
    v.toOption shouldBe None
  }
}
