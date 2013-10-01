package xitrum.annotation

import scala.annotation.StaticAnnotation

object Swagger {
  // https://github.com/wordnik/swagger-core/wiki/Datatypes --------------------

  sealed trait ValueType

  object Byte  extends ValueType { override def toString = "byte" }
  object Int   extends ValueType { override def toString = "integer" }
  object Int32 extends ValueType { override def toString = "int32" }
  object Int64 extends ValueType { override def toString = "int64" }
  object Long  extends ValueType { override def toString = "long" }

  object Number extends ValueType { override def toString = "number" }
  object Float  extends ValueType { override def toString = "float" }
  object Double extends ValueType { override def toString = "double" }

  object String  extends ValueType { override def toString = "string" }
  object Boolean extends ValueType { override def toString = "boolean" }

  object Date     extends ValueType { override def toString = "date" }
  object DateTime extends ValueType { override def toString = "date-time" }

  // https://github.com/wordnik/swagger-core/wiki/Parameters -------------------

  sealed trait ParamType

  object Path   extends ParamType { override def toString = "path" }
  object Query  extends ParamType { override def toString = "query" }
  object Body   extends ParamType { override def toString = "body" }
  object Header extends ParamType { override def toString = "header" }
  object Form   extends ParamType { override def toString = "form" }

  //----------------------------------------------------------------------------

  sealed trait ParamOrResponse extends StaticAnnotation

  case class Param(
    name:      String,
    paramType: ParamType,
    valueType: ValueType,
    desc:      String = ""
  ) extends ParamOrResponse

  case class OptionalParam(
    name:      String,
    paramType: ParamType,
    valueType: ValueType,
    desc:      String = ""
  ) extends ParamOrResponse

  case class Response(
    code: Int,
    desc: String = ""
  ) extends ParamOrResponse
}

case class Swagger(
  desc: String,
  varargs: Swagger.ParamOrResponse*
) extends StaticAnnotation
