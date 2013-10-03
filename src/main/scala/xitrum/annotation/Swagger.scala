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

  case class OptionalBytePath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalIntPath     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt32Path   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt64Path   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalLongPath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalNumberPath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalFloatPath   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDoublePath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalStringPath  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalBooleanPath (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDatePath    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateTimePath(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class OptionalByteQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalIntQuery     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt32Query   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt64Query   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalLongQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalNumberQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalFloatQuery   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDoubleQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalStringQuery  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalBooleanQuery (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateQuery    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateTimeQuery(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class OptionalByteBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalIntBody     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt32Body   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt64Body   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalLongBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalNumberBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalFloatBody   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDoubleBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalStringBody  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalBooleanBody (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateBody    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateTimeBody(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class OptionalByteHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalIntHeader     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt32Header   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt64Header   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalLongHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalNumberHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalFloatHeader   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDoubleHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalStringHeader  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalBooleanHeader (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateHeader    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateTimeHeader(name: String, desc: String = "") extends SwaggerParamOrResponse

  case class OptionalByteForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalIntForm     (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt32Form   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalInt64Form   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalLongForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalNumberForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalFloatForm   (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDoubleForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalStringForm  (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalBooleanForm (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateForm    (name: String, desc: String = "") extends SwaggerParamOrResponse
  case class OptionalDateTimeForm(name: String, desc: String = "") extends SwaggerParamOrResponse
}

case class Swagger(desc: String, varargs: SwaggerParamOrResponse*) extends StaticAnnotation
