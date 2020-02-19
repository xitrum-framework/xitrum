package xitrum

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable.{Map => MMap}

class OptVarPrimitiveTest extends AnyFlatSpec with Matchers {
  behavior of "OptVarPrimitive"

  implicit val action: Action = () => {}

  def newOptVar(): OptVar[Int] = new OptVar[Int] {
    private val all = MMap[String, Any](key -> 123)
    def getAll(implicit action: Action): MMap[String, Any] = all
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
