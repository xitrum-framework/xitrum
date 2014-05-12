package xitrum.scope.request

import scala.reflect.runtime.universe._
import io.netty.handler.codec.http.multipart.FileUpload

import xitrum.Action
import xitrum.exception.MissingParam

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

  // http://groups.google.com/group/scala-user/browse_thread/thread/bb5214c5360b13c3

  sealed class DefaultsTo[A, B]

  trait LowPriorityDefaultsTo {
    implicit def overrideDefault[A, B] = new DefaultsTo[A, B]
  }

  object DefaultsTo extends LowPriorityDefaultsTo {
    implicit def default[B] = new DefaultsTo[B, B]
  }

  //----------------------------------------------------------------------------

  def param[T: TypeTag](key: String, coll: Params = null)(implicit d: T DefaultsTo String): T = {
    if (typeOf[T] <:< TYPE_FILE_UPLOAD) {
      bodyFileParams.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => values(0).asInstanceOf[T]
      }
    } else {
      val coll2 = if (coll == null) textParams else coll
      val value = if (coll2.contains(key)) coll2.apply(key)(0) else throw new MissingParam(key)
      convertText[T](value)
    }
  }

  def paramo[T: TypeTag](key: String, coll: Params = null)(implicit d: T DefaultsTo String): Option[T] = {
    if (typeOf[T] <:< TYPE_FILE_UPLOAD) {
      bodyFileParams.get(key).map { values => values(0).asInstanceOf[T] }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = coll2.get(key)
      val valueo = values.map(_(0))
      valueo.map(convertText[T])
    }
  }

  def params[T: TypeTag](key: String, coll: Params = null)(implicit d: T DefaultsTo String): Seq[T] = {
    if (typeOf[T] <:< TYPE_FILE_UPLOAD) {
      bodyFileParams.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => values.asInstanceOf[Seq[T]]
      }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = if (coll2.contains(key)) coll2.apply(key) else throw new MissingParam(key)
      values.map(convertText[T])
    }
  }

  def paramso[T: TypeTag](key: String, coll: Params = null)(implicit d: T DefaultsTo String): Option[Seq[T]] = {
    if (typeOf[T] <:< TYPE_FILE_UPLOAD) {
      bodyFileParams.get(key).asInstanceOf[Option[Seq[T]]]
    } else {
      val coll2 = if (coll == null) textParams else coll
      coll2.get(key).map(_.map(convertText[T]))
    }
  }

  //----------------------------------------------------------------------------

  /** Applications may override this method to convert to more types. */
  def convertText[T: TypeTag](string: String): T = {
    val t = typeOf[T]
    val any: Any =
           if (t <:< TYPE_STRING)  string
      else if (t <:< TYPE_CHAR)    string(0)
      else if (t <:< TYPE_BOOLEAN) string.toBoolean
      else if (t <:< TYPE_BYTE)    string.toByte
      else if (t <:< TYPE_SHORT)   string.toShort
      else if (t <:< TYPE_INT)     string.toInt
      else if (t <:< TYPE_LONG)    string.toLong
      else if (t <:< TYPE_FLOAT)   string.toFloat
      else if (t <:< TYPE_DOUBLE)  string.toDouble
      else throw new Exception("Cannot covert " + string + " to " + t)
    any.asInstanceOf[T]
  }
}
