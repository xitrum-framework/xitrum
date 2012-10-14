package xitrum.scope.request

import org.jboss.netty.handler.codec.http.multipart.FileUpload

import xitrum.Controller
import xitrum.exception.MissingParam

/**
 * Cache manifests because manifest[T] is a rather expensive operation
 * (several nested objects are created), the same caveat applies at the sender:
 * http://groups.google.com/group/akka-user/browse_thread/thread/ee07764dfc1ac794
 */
object ParamAccess {
  val manifestFileUpload = manifest[FileUpload]
  val manifestString     = manifest[String]
  val manifestInt        = manifest[Int]
  val manifestLong       = manifest[Long]
  val manifestFloat      = manifest[Float]
  val manifestDouble     = manifest[Double]
}

trait ParamAccess {
  this: Controller =>

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
    if (m <:< manifestFileUpload) {
      fileUploadParams.get(key) match {
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
    if (m <:< manifestFileUpload) {
      fileUploadParams.get(key).map { values => values(0).asInstanceOf[T] }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = coll2.get(key)
      val valueo = values.map(_(0))
      valueo.map(convertText[T](_))
    }
  }

  def params[T](key: String, coll: Params = null)(implicit e: T DefaultsTo String, m: Manifest[T]): List[T] = {
    if (m <:< manifestFileUpload) {
      fileUploadParams.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => values.asInstanceOf[List[T]]
      }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = if (coll2.contains(key)) coll2.apply(key) else throw new MissingParam(key)
      values.map(convertText[T](_))
    }
  }

  def paramso[T](key: String, coll: Params = null)(implicit e: T DefaultsTo String, m: Manifest[T]): Option[List[T]] = {
    if (m <:< manifestFileUpload) {
      fileUploadParams.get(key).asInstanceOf[Option[List[T]]]
    } else {
      val coll2 = if (coll == null) textParams else coll
      coll2.get(key).map { values => values.map(convertText[T](_)) }
    }
  }

  //----------------------------------------------------------------------------

  /** Applications may override this method to convert to more types. */
  def convertText[T](value: String)(implicit m: Manifest[T]): T = {
    val any: Any =
           if (m <:< manifestString) value
      else if (m <:< manifestInt)    value.toInt
      else if (m <:< manifestLong)   value.toLong
      else if (m <:< manifestFloat)  value.toFloat
      else if (m <:< manifestDouble) value.toDouble
      else throw new Exception("Cannot covert " + value + " to " + m)

    any.asInstanceOf[T]
  }
}
