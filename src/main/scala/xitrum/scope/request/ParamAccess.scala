package xitrum.scope.request

import scala.reflect.runtime.universe._
import io.netty.handler.codec.http.multipart.FileUpload

import xitrum.Action
import xitrum.exception.MissingParam
import xitrum.util.DefaultsTo

/**
 * Use "manifest" for Scala 2.10 and "typeOf" for Scala 2.11:
 * https://github.com/ngocdaothanh/xitrum/issues/155
 */
object ParamAccess {
  val TYPE_FILE_UPLOAD = typeOf[FileUpload]
  val TYPE_STRING      = typeOf[String]
  val TYPE_CHAR        = typeOf[Char]
  val TYPE_BOOLEAN     = typeOf[Boolean]
  val TYPE_BYTE        = typeOf[Byte]
  val TYPE_SHORT       = typeOf[Short]
  val TYPE_INT         = typeOf[Int]
  val TYPE_LONG        = typeOf[Long]
  val TYPE_FLOAT       = typeOf[Float]
  val TYPE_DOUBLE      = typeOf[Double]
}

trait ParamAccess {
  this: Action =>

  import ParamAccess._

  //----------------------------------------------------------------------------

  def param[T: TypeTag](key: String)(implicit d: T DefaultsTo String): T =
    param(key, textParams)

  def param[T: TypeTag](key: String, coll: Params)(implicit d: T DefaultsTo String): T = {
    if (typeOf[T] <:< TYPE_FILE_UPLOAD) {
      bodyFileParams.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => values.head.asInstanceOf[T]
      }
    } else {
      coll.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => convertTextParam[T](values.head)
      }
    }
  }

  def paramo[T: TypeTag](key: String)(implicit d: T DefaultsTo String): Option[T] =
    paramo(key, textParams)

  def paramo[T: TypeTag](key: String, coll: Params)(implicit d: T DefaultsTo String): Option[T] = {
    if (typeOf[T] <:< TYPE_FILE_UPLOAD) {
      bodyFileParams.get(key).map(_.head.asInstanceOf[T])
    } else {
      coll.get(key).map(values => convertTextParam[T](values.head))
    }
  }

  def params[T: TypeTag](key: String)(implicit d: T DefaultsTo String): Seq[T] =
    params(key, textParams)

  def params[T: TypeTag](key: String, coll: Params)(implicit d: T DefaultsTo String): Seq[T] = {
    if (typeOf[T] <:< TYPE_FILE_UPLOAD) {
      bodyFileParams.get(key) match {
        case None         => Seq.empty
        case Some(values) => values.asInstanceOf[Seq[T]]
      }
    } else {
      coll.get(key) match {
        case None         => Seq.empty
        case Some(values) => values.map(convertTextParam[T])
      }
    }
  }

  //----------------------------------------------------------------------------

  /** Applications may override this method to convert to more types. */
  def convertTextParam[T: TypeTag](value: String): T = {
    val t = typeOf[T]
    val any: Any =
           if (t <:< TYPE_STRING)  value
      else if (t <:< TYPE_CHAR)    value(0)
      else if (t <:< TYPE_BOOLEAN) value.toBoolean
      else if (t <:< TYPE_BYTE)    value.toByte
      else if (t <:< TYPE_SHORT)   value.toShort
      else if (t <:< TYPE_INT)     value.toInt
      else if (t <:< TYPE_LONG)    value.toLong
      else if (t <:< TYPE_FLOAT)   value.toFloat
      else if (t <:< TYPE_DOUBLE)  value.toDouble
      else throw new Exception("convertTextParam cannot covert " + value + " to " + t)
    any.asInstanceOf[T]
  }
}
