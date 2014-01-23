package xitrum.scope.request

import io.netty.handler.codec.http.multipart.FileUpload

import xitrum.Action
import xitrum.exception.MissingParam

// See https://github.com/ngocdaothanh/xitrum/issues/155

/**
 * Cache manifests because manifest[T] is a rather expensive operation
 * (several nested objects are created), the same caveat applies at the sender:
 * http://groups.google.com/group/akka-user/browse_thread/thread/ee07764dfc1ac794
 */
object ParamAccess {
  val MANIFEST_FILE_UPLOAD = manifest[FileUpload]
  val MANIFEST_STRING      = manifest[String]
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

  def param[T](key: String, coll: Params = null)(implicit e: T DefaultsTo String, m: Manifest[T]): T = {
    if (m <:< MANIFEST_FILE_UPLOAD) {
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

  def paramo[T](key: String, coll: Params = null)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[T] = {
    if (m <:< MANIFEST_FILE_UPLOAD) {
      bodyFileParams.get(key).map { values => values(0).asInstanceOf[T] }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = coll2.get(key)
      val valueo = values.map(_(0))
      valueo.map(convertText[T](_))
    }
  }

  def params[T](key: String, coll: Params = null)(implicit e: T DefaultsTo String, m: Manifest[T]): Seq[T] = {
    if (m <:< MANIFEST_FILE_UPLOAD) {
      bodyFileParams.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => values.asInstanceOf[Seq[T]]
      }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = if (coll2.contains(key)) coll2.apply(key) else throw new MissingParam(key)
      values.map(convertText[T](_))
    }
  }

  def paramso[T](key: String, coll: Params = null)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[Seq[T]] = {
    if (m <:< MANIFEST_FILE_UPLOAD) {
      bodyFileParams.get(key).asInstanceOf[Option[Seq[T]]]
    } else {
      val coll2 = if (coll == null) textParams else coll
      coll2.get(key).map { values => values.map(convertText[T](_)) }
    }
  }

  //----------------------------------------------------------------------------

  /** Applications may override this method to convert to more types. */
  def convertText[T](value: String)(implicit m: Manifest[T]): T = {
    val any: Any =
           if (m <:< MANIFEST_STRING) value
      else if (m <:< MANIFEST_INT)    value.toInt
      else if (m <:< MANIFEST_LONG)   value.toLong
      else if (m <:< MANIFEST_FLOAT)  value.toFloat
      else if (m <:< MANIFEST_DOUBLE) value.toDouble
      else throw new Exception("Cannot covert " + value + " to " + m)

    any.asInstanceOf[T]
  }
}
