package xitrum.validation

import scala.collection.mutable.ArrayBuffer

import xitrum.Action
import xitrum.util.SecureBase64

object ValidatorInjector {
  /** @return Param name that has been encrypted to include serialized validators */
  def injectToParamName(paramName: String, validators: Validator*): String = {
    // validators is a WrappedArray
    // For smaller size, use Java array
    //
    // +: http://groups.google.com/group/scala-user/browse_thread/thread/d38dcf5b9e6e3c94
    val jArray = (paramName +: validators).toArray

    SecureBase64.encrypt(jArray)
  }

  /** Take out the original param name and validators from secure param name. */
  def takeOutFromName(secureParamName: String): Option[(String, Iterable[Validator])] =
    SecureBase64.decrypt(secureParamName) match {
      case None => None

      case Some(array) =>
        try {
          val array2    = array.asInstanceOf[Array[Any]]
          val paramName = array2(0).asInstanceOf[String]

          val validators = ArrayBuffer[Validator]()
          for (i <- 1 until array2.length) {
            validators.append(array2(i).asInstanceOf[Validator])
          }
          Some((paramName, validators))
        } catch {
          case e => None
        }
    }
}
