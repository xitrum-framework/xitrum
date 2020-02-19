package xitrum.scope.request

import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._

import io.netty.handler.codec.http.multipart.FileUpload

import xitrum.Action
import xitrum.exception.{InvalidInput, MissingParam}
import xitrum.util.DefaultsTo

/**
 * Use "manifest" for Scala 2.10 and "typeOf" for Scala 2.11:
 * https://github.com/ngocdaothanh/xitrum/issues/155
 */
object ParamAccess {
  val TYPE_FILE_UPLOAD: universe.Type = typeOf[FileUpload]
  val TYPE_STRING     : universe.Type = typeOf[String]
  val TYPE_CHAR       : universe.Type = typeOf[Char]
  val TYPE_BOOLEAN    : universe.Type = typeOf[Boolean]
  val TYPE_BYTE       : universe.Type = typeOf[Byte]
  val TYPE_SHORT      : universe.Type = typeOf[Short]
  val TYPE_INT        : universe.Type = typeOf[Int]
  val TYPE_LONG       : universe.Type = typeOf[Long]
  val TYPE_FLOAT      : universe.Type = typeOf[Float]
  val TYPE_DOUBLE     : universe.Type = typeOf[Double]
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
        case Some(values) => convertTextParam[T](key, values.head)
      }
    }
  }

  def paramo[T: TypeTag](key: String)(implicit d: T DefaultsTo String): Option[T] =
    paramo(key, textParams)

  def paramo[T: TypeTag](key: String, coll: Params)(implicit d: T DefaultsTo String): Option[T] = {
    if (typeOf[T] <:< TYPE_FILE_UPLOAD) {
      bodyFileParams.get(key).map(_.head.asInstanceOf[T])
    } else {
      coll.get(key).map(values => convertTextParam[T](key, values.head))
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
        case Some(values) => values.map(convertTextParam[T](key, _))
      }
    }
  }

  //----------------------------------------------------------------------------

  /** Applications may override this method to convert to more types. */
  def convertTextParam[T: TypeTag](key: String, value: String): T = {
    val t = typeOf[T]
    scala.util.Try[Any] {
           if (t <:< TYPE_STRING)  value
      else if (t <:< TYPE_CHAR)    value(0)
      else if (t <:< TYPE_BOOLEAN) value.toBoolean
      else if (t <:< TYPE_BYTE)    value.toByte
      else if (t <:< TYPE_SHORT)   value.toShort
      else if (t <:< TYPE_INT)     value.toInt
      else if (t <:< TYPE_LONG)    value.toLong
      else if (t <:< TYPE_FLOAT)   value.toFloat
      else if (t <:< TYPE_DOUBLE)  value.toDouble
      else throw new Exception(s"convertTextParam $key cannot convert $value to $t")
    }.recoverWith {
      case _: StringIndexOutOfBoundsException |
           _: IllegalArgumentException =>
        scala.util.Failure(InvalidInput(s"""Cannot convert "$value" of param "$key" to $t"""))
    }.get.asInstanceOf[T]
  }
}
