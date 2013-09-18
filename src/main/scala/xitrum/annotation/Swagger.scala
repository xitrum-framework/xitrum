package xitrum.annotation

import scala.annotation.StaticAnnotation

object Swagger {
  sealed trait ParamOrResponse extends StaticAnnotation

  case class Param(
    name:        String,
    paramType:   String = "path",
    valueType:   String,
    description: String = ""
  ) extends ParamOrResponse

  case class OptionalParam(
    name:        String,
    paramType:   String = "path",
    valueType:   String,
    description: String = ""
  ) extends ParamOrResponse

  case class Response(
    code:    Int,
    message: String
  ) extends ParamOrResponse
}

class Swagger(
  summary: String,
  varargs: Swagger.ParamOrResponse*
) extends StaticAnnotation
