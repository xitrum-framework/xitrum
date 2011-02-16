package xitrum.action.validation

import scala.collection.mutable.ArrayBuffer
import scala.xml.{Attribute, Elem, Text, Null}

import xitrum.action.Action
import xitrum.action.env.session.SecureBase64

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
    val jArray           = validators.+:(paramName).toArray  // +: prepend
    val securedParamName = SecureBase64.serialize(jArray)

    val attr  = Attribute(None, "name", Text(securedParamName), Null)
    val elem2 = elem % attr

    (elem2, paramName, securedParamName)
  }

  /** Take out the original param name and validators from secured param name. */
  def takeOutFromName(securedParamName: String): Option[(String, Iterable[Validator])] =
    SecureBase64.deserialize(securedParamName) match {
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

class ValidatorInjector(action: Action, elem: Elem) {
  import ValidatorInjector._

  def validate(validators: Validator*): Elem = {
    val (elem2, paramName, securedParamName) = injectToParamName(elem, validators:_*)
    for (v <- validators) v.render(action, paramName, securedParamName)
    elem2
  }
}
