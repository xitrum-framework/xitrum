package xitrum.validation

import scala.collection.mutable.ArrayBuffer
import scala.xml.{Attribute, Elem, Text, Null}

import xitrum.Action
import xitrum.scope.session.SecureBase64

object ValidatorInjector {
  /** Replace the original param name with the serialized validators. */
  def injectToParamName(elem: Elem, validators: Validator*): (Elem, String, String) = {
    val nodeSeq = elem \ "@name"
    if (nodeSeq.isEmpty) {
      throw new Exception("The element must have \"name\" attribute")
    }
    val paramName = nodeSeq.head.text  // String

    // validators is a WrappedArray
    // For smaller size, use Java array
    //
    // +: is for prepending, but it's strange that
    // Array(1, 2) +: "hello" is different from
    // Array(1, 2).+:("hello")
    val jArray = (validators.+:(paramName)).toArray

    val securedParamName = SecureBase64.encrypt(jArray)

    val attr  = Attribute(None, "name", Text(securedParamName), Null)
    val elem2 = elem % attr

    (elem2, paramName, securedParamName)
  }

  /** Take out the original param name and validators from secured param name. */
  def takeOutFromName(securedParamName: String): Option[(String, Iterable[Validator])] =
    SecureBase64.decrypt(securedParamName) match {
      case None => None

      case Some(array) =>
        try {
          val array2    = array.asInstanceOf[Array[Any]]
          val paramName = array2(0).asInstanceOf[String]

          val validators = new ArrayBuffer[Validator]
          for (i <- 1 until array2.length) {
            validators.append(array2(i).asInstanceOf[Validator])
          }
          Some((paramName, validators))
        } catch {
          case e => None
        }
    }
}
