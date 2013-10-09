package xitrum.annotation

import scala.annotation.StaticAnnotation

// Put outside object Swagger so that in IDE like Eclipse, when typing Swagger.
// ParamOrResponse is not shown, only appropriate things are shown
sealed trait SwaggerParamOrResponse

object Swagger {
  // See:
  // https://github.com/wordnik/swagger-core/wiki/Datatypes
  // https://github.com/wordnik/swagger-core/wiki/Parameters

  case class Response(code: Int, desc: String) extends SwaggerParamOrResponse

  //----------------------------------------------------------------------------

  case class BytePath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class IntPath     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int32Path   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int64Path   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class LongPath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class NumberPath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class FloatPath   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DoublePath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class StringPath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class BooleanPath (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DatePath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateTimePath(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class ByteQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class IntQuery     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int32Query   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int64Query   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class LongQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class NumberQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class FloatQuery   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DoubleQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class StringQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class BooleanQuery (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateTimeQuery(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class ByteBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class IntBody     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int32Body   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int64Body   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class LongBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class NumberBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class FloatBody   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DoubleBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class StringBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class BooleanBody (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateTimeBody(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class ByteHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class IntHeader     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int32Header   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int64Header   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class LongHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class NumberHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class FloatHeader   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DoubleHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class StringHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class BooleanHeader (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateTimeHeader(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class ByteForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class IntForm     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int32Form   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class Int64Form   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class LongForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class NumberForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class FloatForm   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DoubleForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class StringForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class BooleanForm (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class DateTimeForm(name: String, desc: String = "") extends SwaggerParamOrResponse

  //----------------------------------------------------------------------------

  case class OptBytePath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptIntPath     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt32Path   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt64Path   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptLongPath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptNumberPath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptFloatPath   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDoublePath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptStringPath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptBooleanPath (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDatePath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateTimePath(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class OptByteQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptIntQuery     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt32Query   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt64Query   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptLongQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptNumberQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptFloatQuery   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDoubleQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptStringQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptBooleanQuery (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateTimeQuery(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class OptByteBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptIntBody     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt32Body   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt64Body   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptLongBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptNumberBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptFloatBody   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDoubleBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptStringBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptBooleanBody (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateTimeBody(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class OptByteHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptIntHeader     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt32Header   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt64Header   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptLongHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptNumberHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptFloatHeader   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDoubleHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptStringHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptBooleanHeader (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateTimeHeader(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class OptByteForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptIntForm     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt32Form   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptInt64Form   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptLongForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptNumberForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptFloatForm   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDoubleForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptStringForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptBooleanForm (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptDateTimeForm(name: String, desc: String = "") extends SwaggerParamOrResponse
}

case class Swagger(desc: String, varargs: SwaggerParamOrResponse*) extends StaticAnnotation
