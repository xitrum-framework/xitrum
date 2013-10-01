package xitrum.annotation

import scala.annotation.StaticAnnotation

object Swagger {
  // https://github.com/wordnik/swagger-core/wiki/Datatypes --------------------

  sealed trait ValueType

  object Byte  extends ValueType
  object Int   extends ValueType
  object Int32 extends ValueType
  object Int64 extends ValueType
  object Long  extends ValueType

  object Number extends ValueType
  object Float  extends ValueType
  object Double extends ValueType

  object String  extends ValueType
  object Boolean extends ValueType

  object Date     extends ValueType
  object DateTime extends ValueType

  // https://github.com/wordnik/swagger-core/wiki/Parameters -------------------

  sealed trait ParamType

  object Path   extends ParamType
  object Query  extends ParamType
  object Body   extends ParamType
  object Header extends ParamType
  object Form   extends ParamType

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

class Swagger(
  summary: String,
  varargs: Swagger.ParamOrResponse*
) extends StaticAnnotation
