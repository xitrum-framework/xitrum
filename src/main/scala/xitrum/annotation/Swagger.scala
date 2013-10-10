package xitrum.annotation

import scala.annotation.StaticAnnotation

// Put outside object Swagger so that in IDE like Eclipse, when typing Swagger.
// ParamOrResponse is not shown, only appropriate things are shown
sealed trait SwaggerNoteOrParamOrResponse

/**
 * See:
 * https://github.com/wordnik/swagger-core/wiki/API-Declaration
 * https://github.com/wordnik/swagger-core/wiki/Datatypes
 * https://github.com/wordnik/swagger-core/wiki/Parameters
 */
object Swagger {
  case class Note(note: String) extends SwaggerNoteOrParamOrResponse

  case class Response(code: Int, desc: String) extends SwaggerNoteOrParamOrResponse

  //----------------------------------------------------------------------------

  case class BytePath    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class IntPath     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int32Path   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int64Path   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class LongPath    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class NumberPath  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class FloatPath   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DoublePath  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class StringPath  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class BooleanPath (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DatePath    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateTimePath(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  case class ByteQuery    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class IntQuery     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int32Query   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int64Query   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class LongQuery    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class NumberQuery  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class FloatQuery   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DoubleQuery  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class StringQuery  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class BooleanQuery (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateQuery    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateTimeQuery(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  case class ByteBody    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class IntBody     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int32Body   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int64Body   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class LongBody    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class NumberBody  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class FloatBody   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DoubleBody  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class StringBody  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class BooleanBody (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateBody    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateTimeBody(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  case class ByteHeader    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class IntHeader     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int32Header   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int64Header   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class LongHeader    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class NumberHeader  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class FloatHeader   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DoubleHeader  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class StringHeader  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class BooleanHeader (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateHeader    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateTimeHeader(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  case class ByteForm    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class IntForm     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int32Form   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class Int64Form   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class LongForm    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class NumberForm  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class FloatForm   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DoubleForm  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class StringForm  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class BooleanForm (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateForm    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class DateTimeForm(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  //----------------------------------------------------------------------------

  case class OptBytePath    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptIntPath     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt32Path   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt64Path   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptLongPath    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptNumberPath  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptFloatPath   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDoublePath  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptStringPath  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptBooleanPath (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDatePath    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateTimePath(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  case class OptByteQuery    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptIntQuery     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt32Query   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt64Query   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptLongQuery    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptNumberQuery  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptFloatQuery   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDoubleQuery  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptStringQuery  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptBooleanQuery (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateQuery    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateTimeQuery(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  case class OptByteBody    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptIntBody     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt32Body   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt64Body   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptLongBody    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptNumberBody  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptFloatBody   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDoubleBody  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptStringBody  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptBooleanBody (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateBody    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateTimeBody(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  case class OptByteHeader    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptIntHeader     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt32Header   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt64Header   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptLongHeader    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptNumberHeader  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptFloatHeader   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDoubleHeader  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptStringHeader  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptBooleanHeader (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateHeader    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateTimeHeader(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse

  case class OptByteForm    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptIntForm     (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt32Form   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptInt64Form   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptLongForm    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptNumberForm  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptFloatForm   (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDoubleForm  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptStringForm  (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptBooleanForm (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateForm    (name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
  case class OptDateTimeForm(name: String, desc: String = "") extends SwaggerNoteOrParamOrResponse
}

case class Swagger(summary: String, varargs: SwaggerNoteOrParamOrResponse*) extends StaticAnnotation
