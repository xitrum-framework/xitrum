package xitrum.scope.request

import io.netty.handler.codec.http.multipart.FileUpload

import xitrum.Action
import xitrum.exception.MissingParam

/**
 * Use "manifest" for Scala 2.10 and "typeOf" for Scala 2.11:
 * https://github.com/ngocdaothanh/xitrum/issues/155
 *
 * Cache manifests because manifest[T] is a rather expensive operation
 * (several nested objects are created), the same caveat applies at the sender:
 * http://groups.google.com/group/akka-user/browse_thread/thread/ee07764dfc1ac794
 */
object ParamAccess {
  val MANIFEST_FILE_UPLOAD = manifest[FileUpload]
  val MANIFEST_STRING      = manifest[String]
  val MANIFEST_CHAR        = manifest[Char]
  val MANIFEST_BOOLEAN     = manifest[Boolean]
  val MANIFEST_BYTE        = manifest[Byte]
  val MANIFEST_SHORT       = manifest[Short]
  val MANIFEST_INT         = manifest[Int]
  val MANIFEST_LONG        = manifest[Long]
  val MANIFEST_FLOAT       = manifest[Float]
  val MANIFEST_DOUBLE      = manifest[Double]
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

  def param[T](key: String)(implicit e: T DefaultsTo String, m: Manifest[T]): T =
    param(key, textParams)

  def param[T](key: String, coll: Params)(implicit e: T DefaultsTo String, m: Manifest[T]): T = {
    if (m <:< MANIFEST_FILE_UPLOAD) {
      bodyFileParams.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => values(0).asInstanceOf[T]
      }
    } else {
      val value = if (coll.contains(key)) coll(key).head else throw new MissingParam(key)
      convertTextParam[T](value)
    }
  }

  def paramo[T](key: String)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] =
    paramo(key, textParams)

  def paramo[T](key: String, coll: Params)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] = {
    if (m <:< MANIFEST_FILE_UPLOAD) {
      bodyFileParams.get(key).map { values => values(0).asInstanceOf[T] }
    } else {
      coll.get(key) match {
        case None =>
          None
        case Some(values) =>
          val value = values.head
          if (value.isEmpty) None else Some(convertTextParam[T](value))
      }
    }
  }

  def params[T](key: String)(implicit e: T DefaultsTo String, m: Manifest[T]): Seq[T] =
    params(key, textParams)

  def params[T](key: String, coll: Params)(implicit e: T DefaultsTo String, m: Manifest[T]): Seq[T] = {
    if (m <:< MANIFEST_FILE_UPLOAD) {
      bodyFileParams.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => values.asInstanceOf[Seq[T]]
      }
    } else {
      val values   = if (coll.contains(key)) coll(key) else throw new MissingParam(key)
      val nonEmpty = values.filterNot(_.isEmpty)
      nonEmpty.map(convertTextParam[T])
    }
  }

  def paramso[T](key: String)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[Seq[T]] =
    paramso(key, textParams)

  def paramso[T](key: String, coll: Params)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[Seq[T]] = {
    if (m <:< MANIFEST_FILE_UPLOAD) {
      bodyFileParams.get(key).asInstanceOf[Option[Seq[T]]]
    } else {
      coll.get(key) match {
        case None =>
          None
        case Some(values) =>
          val nonEmpty = values.filterNot(_.isEmpty)
          Some(nonEmpty.map(convertTextParam[T]))
      }
    }
  }

  //----------------------------------------------------------------------------

  /** Applications may override this method to convert to more types. */
  def convertTextParam[T](value: String)(implicit m: Manifest[T]): T = {
    val any: Any =
           if (m <:< MANIFEST_STRING)  value
      else if (m <:< MANIFEST_CHAR)    value(0)
      else if (m <:< MANIFEST_BOOLEAN) value.toBoolean
      else if (m <:< MANIFEST_BYTE)    value.toByte
      else if (m <:< MANIFEST_SHORT)   value.toShort
      else if (m <:< MANIFEST_INT)     value.toInt
      else if (m <:< MANIFEST_LONG)    value.toLong
      else if (m <:< MANIFEST_FLOAT)   value.toFloat
      else if (m <:< MANIFEST_DOUBLE)  value.toDouble
      else throw new Exception("convertTextParam cannot covert " + value + " to " + m)
    any.asInstanceOf[T]
  }
}
