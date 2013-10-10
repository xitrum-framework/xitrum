package xitrum.annotation

import scala.annotation.StaticAnnotation

// Put outside object Swagger so that in IDE like Eclipse, when typing Swagger.
// ParamOrResponse is not shown, only appropriate things are shown
sealed trait SwaggerArg

case class Swagger(varargs: SwaggerArg*) extends StaticAnnotation

/**
 * See:
 * https://github.com/wordnik/swagger-core/wiki/API-Declaration
 * https://github.com/wordnik/swagger-core/wiki/Datatypes
 * https://github.com/wordnik/swagger-core/wiki/Parameters
 */
object Swagger {
  case class Summary(summary: String) extends SwaggerArg

  case class Note(note: String) extends SwaggerArg

  case class Response(code: Int, desc: String) extends SwaggerArg

  //----------------------------------------------------------------------------

  case class BytePath    (name: String, desc: String = "") extends SwaggerArg
  case class IntPath     (name: String, desc: String = "") extends SwaggerArg
  case class Int32Path   (name: String, desc: String = "") extends SwaggerArg
  case class Int64Path   (name: String, desc: String = "") extends SwaggerArg
  case class LongPath    (name: String, desc: String = "") extends SwaggerArg
  case class NumberPath  (name: String, desc: String = "") extends SwaggerArg
  case class FloatPath   (name: String, desc: String = "") extends SwaggerArg
  case class DoublePath  (name: String, desc: String = "") extends SwaggerArg
  case class StringPath  (name: String, desc: String = "") extends SwaggerArg
  case class BooleanPath (name: String, desc: String = "") extends SwaggerArg
  case class DatePath    (name: String, desc: String = "") extends SwaggerArg
  case class DateTimePath(name: String, desc: String = "") extends SwaggerArg

  case class ByteQuery    (name: String, desc: String = "") extends SwaggerArg
  case class IntQuery     (name: String, desc: String = "") extends SwaggerArg
  case class Int32Query   (name: String, desc: String = "") extends SwaggerArg
  case class Int64Query   (name: String, desc: String = "") extends SwaggerArg
  case class LongQuery    (name: String, desc: String = "") extends SwaggerArg
  case class NumberQuery  (name: String, desc: String = "") extends SwaggerArg
  case class FloatQuery   (name: String, desc: String = "") extends SwaggerArg
  case class DoubleQuery  (name: String, desc: String = "") extends SwaggerArg
  case class StringQuery  (name: String, desc: String = "") extends SwaggerArg
  case class BooleanQuery (name: String, desc: String = "") extends SwaggerArg
  case class DateQuery    (name: String, desc: String = "") extends SwaggerArg
  case class DateTimeQuery(name: String, desc: String = "") extends SwaggerArg

  case class ByteBody    (name: String, desc: String = "") extends SwaggerArg
  case class IntBody     (name: String, desc: String = "") extends SwaggerArg
  case class Int32Body   (name: String, desc: String = "") extends SwaggerArg
  case class Int64Body   (name: String, desc: String = "") extends SwaggerArg
  case class LongBody    (name: String, desc: String = "") extends SwaggerArg
  case class NumberBody  (name: String, desc: String = "") extends SwaggerArg
  case class FloatBody   (name: String, desc: String = "") extends SwaggerArg
  case class DoubleBody  (name: String, desc: String = "") extends SwaggerArg
  case class StringBody  (name: String, desc: String = "") extends SwaggerArg
  case class BooleanBody (name: String, desc: String = "") extends SwaggerArg
  case class DateBody    (name: String, desc: String = "") extends SwaggerArg
  case class DateTimeBody(name: String, desc: String = "") extends SwaggerArg

  case class ByteHeader    (name: String, desc: String = "") extends SwaggerArg
  case class IntHeader     (name: String, desc: String = "") extends SwaggerArg
  case class Int32Header   (name: String, desc: String = "") extends SwaggerArg
  case class Int64Header   (name: String, desc: String = "") extends SwaggerArg
  case class LongHeader    (name: String, desc: String = "") extends SwaggerArg
  case class NumberHeader  (name: String, desc: String = "") extends SwaggerArg
  case class FloatHeader   (name: String, desc: String = "") extends SwaggerArg
  case class DoubleHeader  (name: String, desc: String = "") extends SwaggerArg
  case class StringHeader  (name: String, desc: String = "") extends SwaggerArg
  case class BooleanHeader (name: String, desc: String = "") extends SwaggerArg
  case class DateHeader    (name: String, desc: String = "") extends SwaggerArg
  case class DateTimeHeader(name: String, desc: String = "") extends SwaggerArg

  case class ByteForm    (name: String, desc: String = "") extends SwaggerArg
  case class IntForm     (name: String, desc: String = "") extends SwaggerArg
  case class Int32Form   (name: String, desc: String = "") extends SwaggerArg
  case class Int64Form   (name: String, desc: String = "") extends SwaggerArg
  case class LongForm    (name: String, desc: String = "") extends SwaggerArg
  case class NumberForm  (name: String, desc: String = "") extends SwaggerArg
  case class FloatForm   (name: String, desc: String = "") extends SwaggerArg
  case class DoubleForm  (name: String, desc: String = "") extends SwaggerArg
  case class StringForm  (name: String, desc: String = "") extends SwaggerArg
  case class BooleanForm (name: String, desc: String = "") extends SwaggerArg
  case class DateForm    (name: String, desc: String = "") extends SwaggerArg
  case class DateTimeForm(name: String, desc: String = "") extends SwaggerArg

  //----------------------------------------------------------------------------

  case class OptBytePath    (name: String, desc: String = "") extends SwaggerArg
  case class OptIntPath     (name: String, desc: String = "") extends SwaggerArg
  case class OptInt32Path   (name: String, desc: String = "") extends SwaggerArg
  case class OptInt64Path   (name: String, desc: String = "") extends SwaggerArg
  case class OptLongPath    (name: String, desc: String = "") extends SwaggerArg
  case class OptNumberPath  (name: String, desc: String = "") extends SwaggerArg
  case class OptFloatPath   (name: String, desc: String = "") extends SwaggerArg
  case class OptDoublePath  (name: String, desc: String = "") extends SwaggerArg
  case class OptStringPath  (name: String, desc: String = "") extends SwaggerArg
  case class OptBooleanPath (name: String, desc: String = "") extends SwaggerArg
  case class OptDatePath    (name: String, desc: String = "") extends SwaggerArg
  case class OptDateTimePath(name: String, desc: String = "") extends SwaggerArg

  case class OptByteQuery    (name: String, desc: String = "") extends SwaggerArg
  case class OptIntQuery     (name: String, desc: String = "") extends SwaggerArg
  case class OptInt32Query   (name: String, desc: String = "") extends SwaggerArg
  case class OptInt64Query   (name: String, desc: String = "") extends SwaggerArg
  case class OptLongQuery    (name: String, desc: String = "") extends SwaggerArg
  case class OptNumberQuery  (name: String, desc: String = "") extends SwaggerArg
  case class OptFloatQuery   (name: String, desc: String = "") extends SwaggerArg
  case class OptDoubleQuery  (name: String, desc: String = "") extends SwaggerArg
  case class OptStringQuery  (name: String, desc: String = "") extends SwaggerArg
  case class OptBooleanQuery (name: String, desc: String = "") extends SwaggerArg
  case class OptDateQuery    (name: String, desc: String = "") extends SwaggerArg
  case class OptDateTimeQuery(name: String, desc: String = "") extends SwaggerArg

  case class OptByteBody    (name: String, desc: String = "") extends SwaggerArg
  case class OptIntBody     (name: String, desc: String = "") extends SwaggerArg
  case class OptInt32Body   (name: String, desc: String = "") extends SwaggerArg
  case class OptInt64Body   (name: String, desc: String = "") extends SwaggerArg
  case class OptLongBody    (name: String, desc: String = "") extends SwaggerArg
  case class OptNumberBody  (name: String, desc: String = "") extends SwaggerArg
  case class OptFloatBody   (name: String, desc: String = "") extends SwaggerArg
  case class OptDoubleBody  (name: String, desc: String = "") extends SwaggerArg
  case class OptStringBody  (name: String, desc: String = "") extends SwaggerArg
  case class OptBooleanBody (name: String, desc: String = "") extends SwaggerArg
  case class OptDateBody    (name: String, desc: String = "") extends SwaggerArg
  case class OptDateTimeBody(name: String, desc: String = "") extends SwaggerArg

  case class OptByteHeader    (name: String, desc: String = "") extends SwaggerArg
  case class OptIntHeader     (name: String, desc: String = "") extends SwaggerArg
  case class OptInt32Header   (name: String, desc: String = "") extends SwaggerArg
  case class OptInt64Header   (name: String, desc: String = "") extends SwaggerArg
  case class OptLongHeader    (name: String, desc: String = "") extends SwaggerArg
  case class OptNumberHeader  (name: String, desc: String = "") extends SwaggerArg
  case class OptFloatHeader   (name: String, desc: String = "") extends SwaggerArg
  case class OptDoubleHeader  (name: String, desc: String = "") extends SwaggerArg
  case class OptStringHeader  (name: String, desc: String = "") extends SwaggerArg
  case class OptBooleanHeader (name: String, desc: String = "") extends SwaggerArg
  case class OptDateHeader    (name: String, desc: String = "") extends SwaggerArg
  case class OptDateTimeHeader(name: String, desc: String = "") extends SwaggerArg

  case class OptByteForm    (name: String, desc: String = "") extends SwaggerArg
  case class OptIntForm     (name: String, desc: String = "") extends SwaggerArg
  case class OptInt32Form   (name: String, desc: String = "") extends SwaggerArg
  case class OptInt64Form   (name: String, desc: String = "") extends SwaggerArg
  case class OptLongForm    (name: String, desc: String = "") extends SwaggerArg
  case class OptNumberForm  (name: String, desc: String = "") extends SwaggerArg
  case class OptFloatForm   (name: String, desc: String = "") extends SwaggerArg
  case class OptDoubleForm  (name: String, desc: String = "") extends SwaggerArg
  case class OptStringForm  (name: String, desc: String = "") extends SwaggerArg
  case class OptBooleanForm (name: String, desc: String = "") extends SwaggerArg
  case class OptDateForm    (name: String, desc: String = "") extends SwaggerArg
  case class OptDateTimeForm(name: String, desc: String = "") extends SwaggerArg
}
