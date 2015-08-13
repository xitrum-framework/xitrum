package xitrum.annotation

import scala.annotation.StaticAnnotation

case class Swagger(swaggerArgs: SwaggerTypes.SwaggerArg*) extends StaticAnnotation

// Put outside object Swagger so that in IDEs like Eclipse, when typing Swagger<dot>,
// SwaggerArg won't be shown, only concrete SwaggerArgs are shown.
object SwaggerTypes {
  sealed trait SwaggerArg

  sealed trait SwaggerParamArg extends SwaggerArg {
    def name: String
    def desc: String
  }

  sealed trait SwaggerOptParamArg extends SwaggerParamArg

  sealed trait SwaggerPathParam
  sealed trait SwaggerQueryParam
  sealed trait SwaggerHeaderParam
  sealed trait SwaggerFormParam
  sealed trait SwaggerBodyParam

  sealed trait SwaggerIntParam
  sealed trait SwaggerLongParam
  sealed trait SwaggerFloatParam
  sealed trait SwaggerDoubleParam
  sealed trait SwaggerStringParam
  sealed trait SwaggerByteParam
  sealed trait SwaggerBinaryParam
  sealed trait SwaggerBooleanParam
  sealed trait SwaggerDateParam
  sealed trait SwaggerDateTimeParam
  sealed trait SwaggerPasswordParam
  sealed trait SwaggerFileParam
}

/** See: https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md */
object Swagger {
  import SwaggerTypes._

  case class Tags(tags: String*) extends SwaggerArg
  case class Summary(summary: String) extends SwaggerArg
  case class Description(desc: String) extends SwaggerArg
  case class ExternalDocs(url: String, desc: String = "") extends SwaggerArg
  case class OperationId(id: String) extends SwaggerArg

  case class Consumes(contentTypes: String*) extends SwaggerArg
  case class Produces(contentTypes: String*) extends SwaggerArg

  /** Can use this multiple times at an action. */
  case class Response(code: Int = 0, desc: String) extends SwaggerArg

  case class Schemes(schemes: String*) extends SwaggerArg
  case object Deprecated extends SwaggerArg

  //----------------------------------------------------------------------------

  case class IntPath     (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerIntParam
  case class LongPath    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerLongParam
  case class FloatPath   (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerFloatParam
  case class DoublePath  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerDoubleParam
  case class StringPath  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerStringParam
  case class BytePath    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerByteParam
  case class BinaryPath  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerBinaryParam
  case class BooleanPath (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerBooleanParam
  case class DatePath    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerDateParam
  case class DateTimePath(name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerDateTimeParam
  case class PasswordPath(name: String, desc: String = "") extends SwaggerParamArg with SwaggerPathParam with SwaggerPasswordParam

  case class IntQuery     (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerIntParam
  case class LongQuery    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerLongParam
  case class FloatQuery   (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerFloatParam
  case class DoubleQuery  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerDoubleParam
  case class StringQuery  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerStringParam
  case class ByteQuery    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerByteParam
  case class BinaryQuery  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerBinaryParam
  case class BooleanQuery (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerBooleanParam
  case class DateQuery    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerDateParam
  case class DateTimeQuery(name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerDateTimeParam
  case class PasswordQuery(name: String, desc: String = "") extends SwaggerParamArg with SwaggerQueryParam with SwaggerPasswordParam

  case class IntHeader     (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerIntParam
  case class LongHeader    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerLongParam
  case class FloatHeader   (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerFloatParam
  case class DoubleHeader  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerDoubleParam
  case class StringHeader  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerStringParam
  case class ByteHeader    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerByteParam
  case class BinaryHeader  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerBinaryParam
  case class BooleanHeader (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerBooleanParam
  case class DateHeader    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerDateParam
  case class DateTimeHeader(name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerDateTimeParam
  case class PasswordHeader(name: String, desc: String = "") extends SwaggerParamArg with SwaggerHeaderParam with SwaggerPasswordParam

  case class IntForm     (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerIntParam
  case class LongForm    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerLongParam
  case class FloatForm   (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerFloatParam
  case class DoubleForm  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerDoubleParam
  case class StringForm  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerStringParam
  case class ByteForm    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerByteParam
  case class BinaryForm  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerBinaryParam
  case class BooleanForm (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerBooleanParam
  case class DateForm    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerDateParam
  case class DateTimeForm(name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerDateTimeParam
  case class PasswordForm(name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerPasswordParam
  case class FileForm    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerFormParam with SwaggerFileParam

  case class IntBody     (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerIntParam
  case class LongBody    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerLongParam
  case class FloatBody   (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerFloatParam
  case class DoubleBody  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerDoubleParam
  case class StringBody  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerStringParam
  case class ByteBody    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerByteParam
  case class BinaryBody  (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerBinaryParam
  case class BooleanBody (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerBooleanParam
  case class DateBody    (name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerDateParam
  case class DateTimeBody(name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerDateTimeParam
  case class PasswordBody(name: String, desc: String = "") extends SwaggerParamArg with SwaggerBodyParam with SwaggerPasswordParam

  //----------------------------------------------------------------------------

  case class OptIntPath     (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerIntParam
  case class OptLongPath    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerLongParam
  case class OptFloatPath   (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerFloatParam
  case class OptDoublePath  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerDoubleParam
  case class OptStringPath  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerStringParam
  case class OptBytePath    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerByteParam
  case class OptBinaryPath  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerBinaryParam
  case class OptBooleanPath (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerBooleanParam
  case class OptDatePath    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerDateParam
  case class OptDateTimePath(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerDateTimeParam
  case class OptPasswordPath(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerPathParam with SwaggerPasswordParam

  case class OptIntQuery     (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerIntParam
  case class OptLongQuery    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerLongParam
  case class OptFloatQuery   (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerFloatParam
  case class OptDoubleQuery  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerDoubleParam
  case class OptStringQuery  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerStringParam
  case class OptByteQuery    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerByteParam
  case class OptBinaryQuery  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerBinaryParam
  case class OptBooleanQuery (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerBooleanParam
  case class OptDateQuery    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerDateParam
  case class OptDateTimeQuery(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerDateTimeParam
  case class OptPasswordQuery(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerQueryParam with SwaggerPasswordParam

  case class OptIntHeader     (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerIntParam
  case class OptLongHeader    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerLongParam
  case class OptFloatHeader   (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerFloatParam
  case class OptDoubleHeader  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerDoubleParam
  case class OptStringHeader  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerStringParam
  case class OptByteHeader    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerByteParam
  case class OptBinaryHeader  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerBinaryParam
  case class OptBooleanHeader (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerBooleanParam
  case class OptDateHeader    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerDateParam
  case class OptDateTimeHeader(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerDateTimeParam
  case class OptPasswordHeader(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerHeaderParam with SwaggerPasswordParam

  case class OptIntForm     (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerIntParam
  case class OptLongForm    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerLongParam
  case class OptFloatForm   (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerFloatParam
  case class OptDoubleForm  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerDoubleParam
  case class OptStringForm  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerStringParam
  case class OptByteForm    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerByteParam
  case class OptBinaryForm  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerBinaryParam
  case class OptBooleanForm (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerBooleanParam
  case class OptDateForm    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerDateParam
  case class OptDateTimeForm(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerDateTimeParam
  case class OptPasswordForm(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerPasswordParam
  case class OptFileForm    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerFormParam with SwaggerFileParam

  case class OptIntBody     (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerIntParam
  case class OptLongBody    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerLongParam
  case class OptFloatBody   (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerFloatParam
  case class OptDoubleBody  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerDoubleParam
  case class OptStringBody  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerStringParam
  case class OptByteBody    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerByteParam
  case class OptBinaryBody  (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerBinaryParam
  case class OptBooleanBody (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerBooleanParam
  case class OptDateBody    (name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerDateParam
  case class OptDateTimeBody(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerDateTimeParam
  case class OptPasswordBody(name: String, desc: String = "") extends SwaggerOptParamArg with SwaggerBodyParam with SwaggerPasswordParam
}
