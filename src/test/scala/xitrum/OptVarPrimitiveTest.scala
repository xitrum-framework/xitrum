package xitrum

import scala.collection.mutable.{Map => MMap}
import org.scalatest.{FlatSpec, Matchers}

class OptVarPrimitiveTest extends FlatSpec with Matchers {
  behavior of "OptVarPrimitive"

  implicit val action = new Action { def execute() {} }

  def newOptVar() = new OptVar[Int] {
    private val all = MMap[String, Any](key -> 123)
    def getAll(implicit action: Action) = all
  }

  "get" should "handle primitive" in {
    val v = newOptVar()
    v.get shouldBe 123
  }

  "remove" should "handle primitive" in {
    val v = newOptVar()
    v.remove() shouldBe Some(123)
  }

  "toOption" should "handle primitive" in {
    val v = newOptVar()
    v.toOption shouldBe Some(123)
  }
}
