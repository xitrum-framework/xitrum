package xitrum.scope.request

import scala.reflect.runtime.universe._

import org.jboss.netty.handler.codec.http.multipart.FileUpload

import xitrum.Controller
import xitrum.exception.MissingParam

/**
 * Cache manifests because manifest[T] is a rather expensive operation
 * (several nested objects are created), the same caveat applies at the sender:
 * http://groups.google.com/group/akka-user/browse_thread/thread/ee07764dfc1ac794
 */
object ParamAccess {
  val typeFileUpload = typeOf[FileUpload]
  val typeString     = typeOf[String]
  val typeInt        = typeOf[Int]
  val typeLong       = typeOf[Long]
  val typeFloat      = typeOf[Float]
  val typeDouble     = typeOf[Double]
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

  def param[T: TypeTag](key: String, coll: Params = null)(implicit d: T DefaultsTo String): T = {
    if (typeOf[T] <:< typeFileUpload) {
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

  def paramo[T: TypeTag](key: String, coll: Params = null)(implicit d: T DefaultsTo String): Option[T] = {
    if (typeOf[T] <:< typeFileUpload) {
      fileUploadParams.get(key).map { values => values(0).asInstanceOf[T] }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = coll2.get(key)
      val valueo = values.map(_(0))
      valueo.map(convertText[T](_))
    }
  }

  def params[T: TypeTag](key: String, coll: Params = null)(implicit d: T DefaultsTo String): Seq[T] = {
    if (typeOf[T] <:< typeFileUpload) {
      fileUploadParams.get(key) match {
        case None         => throw new MissingParam(key)
        case Some(values) => values.asInstanceOf[Seq[T]]
      }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = if (coll2.contains(key)) coll2.apply(key) else throw new MissingParam(key)
      values.map(convertText[T](_))
    }
  }

  def paramso[T: TypeTag](key: String, coll: Params = null)(implicit d: T DefaultsTo String): Option[Seq[T]] = {
    if (typeOf[T] <:< typeFileUpload) {
      fileUploadParams.get(key).asInstanceOf[Option[Seq[T]]]
    } else {
      val coll2 = if (coll == null) textParams else coll
      coll2.get(key).map { values => values.map(convertText[T](_)) }
    }
  }

  //----------------------------------------------------------------------------

  /** Applications may override this method to convert to more types. */
  def convertText[T: TypeTag](value: String): T = {
    val t = typeOf[T]
    val any: Any =
           if (t <:< typeString) value
      else if (t <:< typeInt)    value.toInt
      else if (t <:< typeLong)   value.toLong
      else if (t <:< typeFloat)  value.toFloat
      else if (t <:< typeDouble) value.toDouble
      else throw new Exception("Cannot covert " + value + " to " + t)

    any.asInstanceOf[T]
  }
}
