package xitrum.annotation

import org.scalatest.funspec.AsyncFunSpec

import scala.reflect.runtime.universe.{Type, typeOf}

case class Sub1(name: String, value: Int)

case class Sub2(key: String, value: Boolean)

object Namespace {
  case class Sub(name: String, value: Option[Int])
}

case class Test(
  int:       Int,
  long:      Long,
  float:     Float,
  double:    Double,
  string:    String,
  boolean:   Boolean,
  opInt:     Option[Int],
  opLong:    Option[Long],
  opFloat:   Option[Float],
  opDouble:  Option[Double],
  opString:  Option[String],
  opBoolean: Option[Boolean],
  arrInt:    Seq[Int],
  arrLong:   List[Long],
  arrFloat:  Array[Float],
  arrDouble: Set[Double],
  sub:       Sub1,
  opSub:     Option[Sub2],
  arrSub1:   Seq[Sub1],
  arrSub2:   Array[Sub2]
)

class SwaggerReflectionSpec extends AsyncFunSpec {
  object jsonType {
    def int(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(None, "integer", "int32", isArray = isArray)

    def long(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(None, "integer", "int64", isArray = isArray)

    def float(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(None, "number", "float", isArray = isArray)

    def double(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(None, "number", "double", isArray = isArray)

    def string(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(None, "string", isArray = isArray)

    def boolean(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(None, "boolean", isArray = isArray)

    def sub1(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(Some("Sub1"), "object", fields = Seq(
      Swagger.JsonField("name", Swagger.JsonType(None, "string"), isRequired = true),
      Swagger.JsonField("value", Swagger.JsonType(None, "integer", "int32"), isRequired = true)
    ), isArray = isArray)

    def sub2(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(Some("Sub2"), "object", fields = Seq(
      Swagger.JsonField("key", Swagger.JsonType(None, "string"), isRequired = true),
      Swagger.JsonField("value", Swagger.JsonType(None, "boolean"), isRequired = true)
    ), isArray = isArray)

    def sub(isArray : Boolean = false): Swagger.JsonType = Swagger.JsonType(Some("Namespace.Sub"), "object", fields = Seq(
      Swagger.JsonField("name", Swagger.JsonType(None, "string"), isRequired = true),
      Swagger.JsonField("value", Swagger.JsonType(None, "integer", "int32"))
    ), isArray = isArray)
  }

  describe("SwaggerReflection") {

    describe("_reflectField") {
      val tpe = typeOf[Test]

      def call(field: String): Swagger.JsonField = {
        val fields = tpe.decls.filter(_.isPublic)
          .filter(_.isTerm).map(_.asTerm)
          .filter(_.isGetter)
        val fld = fields.filter(_.name.toString == field).head
        SwaggerReflection.reflectField(fld)
      }

      describe("Simple types") {
        it("Int") {
          assertResult(Swagger.JsonField("int", jsonType.int(), isRequired = true)) {
            call("int")
          }
        }

        it("Long") {
          assertResult(Swagger.JsonField("long", jsonType.long(), isRequired = true)) {
            call("long")
          }
        }

        it("Float") {
          assertResult(Swagger.JsonField("float", jsonType.float(), isRequired = true)) {
            call("float")
          }
        }

        it("Double") {
          assertResult(Swagger.JsonField("double", jsonType.double(), isRequired = true)) {
            call("double")
          }
        }

        it("String") {
          assertResult(Swagger.JsonField("string", jsonType.string(), isRequired = true)) {
            call("string")
          }
        }

        it("Boolean") {
          assertResult(Swagger.JsonField("boolean", jsonType.boolean(), isRequired = true)) {
            call("boolean")
          }
        }
      }

      describe("Optional simple types") {
        it("Int") {
          assertResult(Swagger.JsonField("opInt", jsonType.int())) {
            call("opInt")
          }
        }

        it("Long") {
          assertResult(Swagger.JsonField("opLong", jsonType.long())) {
            call("opLong")
          }
        }

        it("Float") {
          assertResult(Swagger.JsonField("opFloat", jsonType.float())) {
            call("opFloat")
          }
        }

        it("Double") {
          assertResult(Swagger.JsonField("opDouble", jsonType.double())) {
            call("opDouble")
          }
        }

        it("String") {
          assertResult(Swagger.JsonField("opString", jsonType.string())) {
            call("opString")
          }
        }

        it("Boolean") {
          assertResult(Swagger.JsonField("opBoolean", jsonType.boolean())) {
            call("opBoolean")
          }
        }
      }

      describe("Simple arrays") {
        it("Seq") {
          assertResult(Swagger.JsonField("arrInt", jsonType.int(true), isRequired = true)) {
            call("arrInt")
          }
        }

        it("List") {
          assertResult(Swagger.JsonField("arrLong", jsonType.long(true), isRequired = true)) {
            call("arrLong")
          }
        }

        it("Array") {
          assertResult(Swagger.JsonField("arrFloat", jsonType.float(true), isRequired = true)) {
            call("arrFloat")
          }
        }

        it("Set") {
          assertResult(Swagger.JsonField("arrDouble", jsonType.double(true), isRequired = true)) {
            call("arrDouble")
          }
        }
      }

      describe("Case classes") {
        it("Sub1") {
          assertResult(Swagger.JsonField("sub", jsonType.sub1(), isRequired = true)) {
            call("sub")
          }
        }
      }

      describe("Optional case classes") {
        it("Sub2") {
          assertResult(Swagger.JsonField("opSub", jsonType.sub2())) {
            call("opSub")
          }
        }
      }

      describe("Case classes arrays") {
        it("Seq") {
          assertResult(Swagger.JsonField("arrSub1", jsonType.sub1(true), isRequired = true)) {
            call("arrSub1")
          }
        }

        it("List") {
          assertResult(Swagger.JsonField("arrSub2", jsonType.sub2(true), isRequired = true)) {
            call("arrSub2")
          }
        }
      }
    }

    describe("_reflectTypeFormat") {
      def call(tpe: Type) = SwaggerReflection.reflectTypeFormat(tpe)

      describe("Simple types") {
        it("Int") {
          assertResult((None, "integer", "int32")) {
            call(typeOf[Int])
          }
        }

        it("Long") {
          assertResult((None, "integer", "int64")) {
            call(typeOf[Long])
          }
        }

        it("Float") {
          assertResult((None, "number", "float")) {
            call(typeOf[Float])
          }
        }

        it("Double") {
          assertResult((None, "number", "double")) {
            call(typeOf[Double])
          }
        }

        it("String") {
          assertResult((None, "string", "")) {
            call(typeOf[String])
          }
        }

        it("Boolean") {
          assertResult((None, "boolean", "")) {
            call(typeOf[Boolean])
          }
        }
      }

      describe("Case classes") {
        it("Sub1") {
          assertResult((Some("Sub1"), "object", "")) {
            call(typeOf[Sub1])
          }
        }

        it("Sub2") {
          assertResult((Some("Sub2"), "object", "")) {
            call(typeOf[Sub2])
          }
        }

        it("Namespace.Sub") {
          assertResult((Some("Namespace.Sub"), "object", "")) {
            call(typeOf[Namespace.Sub])
          }
        }
      }
    }

    describe("isArrayType") {
      def call(tpe: Type) = SwaggerReflection.isArrayType(tpe)

      it("Seq") {
        assert(call(typeOf[Seq[String]]))
        assert(call(typeOf[Seq[Int]]))
        assert(call(typeOf[Seq[(Int, Boolean)]]))
      }

      it("List") {
        assert(call(typeOf[List[Float]]))
        assert(call(typeOf[List[Boolean]]))
        assert(call(typeOf[List[Sub1]]))
      }

      it("Array") {
        assert(call(typeOf[Array[Long]]))
        assert(call(typeOf[Array[String]]))
        assert(call(typeOf[Array[Sub2]]))
      }

      it("Set") {
        assert(call(typeOf[Set[Long]]))
        assert(call(typeOf[Set[String]]))
        assert(call(typeOf[Set[Namespace.Sub]]))
      }
    }

    describe("reflect") {
      def call(tpe: Type) = SwaggerReflection.reflect(tpe)

      describe("Simple Types") {
        it("Int") {
          assertResult(jsonType.int()) {
            call(typeOf[Int])
          }
        }

        it("Long") {
          assertResult(jsonType.long()) {
            call(typeOf[Long])
          }
        }

        it("Float") {
          assertResult(jsonType.float()) {
            call(typeOf[Float])
          }
        }

        it("Double") {
          assertResult(jsonType.double()) {
            call(typeOf[Double])
          }
        }

        it("String") {
          assertResult(jsonType.string()) {
            call(typeOf[String])
          }
        }

        it("Boolean") {
          assertResult(jsonType.boolean()) {
            call(typeOf[Boolean])
          }
        }
      }

      describe("Simple Arrays") {
        it("Seq") {
          assertResult(jsonType.int(true)) {
            call(typeOf[Seq[Int]])
          }
        }

        it("List") {
          assertResult(jsonType.long(true)) {
            call(typeOf[List[Long]])
          }
        }

        it("Array") {
          assertResult(jsonType.float(true)) {
            call(typeOf[Array[Float]])
          }
        }

        it("Set") {
          assertResult(jsonType.double(true)) {
            call(typeOf[Set[Double]])
          }
        }
      }

      describe("Case Classes") {
        it("Sub1") {
          assertResult(jsonType.sub1()) {
            call(typeOf[Sub1])
          }
        }
        it("Sub2") {
          assertResult(jsonType.sub2()) {
            call(typeOf[Sub2])
          }
        }
        it("Namespace.Sub") {
          assertResult(jsonType.sub()) {
            call(typeOf[Namespace.Sub])
          }
        }
      }

      describe("Case Classes Arrays") {
        it("Seq") {
          assertResult(jsonType.sub1(true)) {
            call(typeOf[Seq[Sub1]])
          }
        }

        it("List") {
          assertResult(jsonType.sub2(true)) {
            call(typeOf[List[Sub2]])
          }
        }

        it("Array") {
          assertResult(jsonType.sub(true)) {
            call(typeOf[Array[Namespace.Sub]])
          }
        }

        it("Set") {
          assertResult(jsonType.sub(true)) {
            call(typeOf[Set[Namespace.Sub]])
          }
        }
      }
    }
  }
}
