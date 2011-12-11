package xitrum.scope.request

import io.netty.handler.codec.http.FileUpload

import xitrum.Action
import xitrum.exception.MissingParam

trait ParamAccess {
  this: Action =>

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
    if (m <:< manifest[FileUpload]) {
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
    if (m <:< manifest[FileUpload]) {
      fileUploadParams.get(key).map { values => values(0).asInstanceOf[T] }
    } else {
      val coll2  = if (coll == null) textParams else coll
      val values = coll2.get(key)
      val valueo = if (values.isEmpty) None else Some((values.get)(0))
      valueo.map(convertText[T](_))
    }
  }

  def params[T](key: String, coll: Params = null)(implicit e: T DefaultsTo String, m: Manifest[T]): List[T] = {
    if (m <:< manifest[FileUpload]) {
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
    if (m <:< manifest[FileUpload]) {
      fileUploadParams.get(key).asInstanceOf[Option[List[T]]]
    } else {
      val coll2 = if (coll == null) textParams else coll
      coll2.get(key) match {
        case None         => None
        case Some(values) => Some(values.map(convertText[T](_)))
      }
    }
  }

  //----------------------------------------------------------------------------

  /** Applications may override this method to convert to more types. */
  def convertText[T](value: String)(implicit m: Manifest[T]): T = {
    val any: Any =
           if (m <:< manifest[String]) value
      else if (m <:< manifest[Int])    value.toInt
      else if (m <:< manifest[Long])   value.toLong
      else if (m <:< manifest[Float])  value.toFloat
      else if (m <:< manifest[Double]) value.toDouble
      else throw new Exception("Cannot covert " + value + " to " + m)

    any.asInstanceOf[T]
  }
}
