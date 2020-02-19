package xitrum

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.{Map => MMap}

class OptVarRecoveryTest extends AnyFlatSpec with Matchers {
  behavior of "OptVarRecovery"

  implicit val action: Action = () => {}

  def newOptVar(): OptVar[String] = new OptVar[String] {
    private val all = MMap[String, Any](key -> Seq("blah"))
    def getAll(implicit action: Action): MMap[String, Any] = all
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
