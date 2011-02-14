package xitrum.vc.validator

import scala.collection.mutable.ArrayBuffer
import scala.xml.{Attribute, Elem, Text, Null}

import xitrum.Action
import xitrum.vc.env.session.SecureBase64

object ValidatorInjector {
  def injectToName(elem: Elem, validators: Validator*): (Elem, String, String) = {
    val nodeSeq = elem \ "@name"
    if (nodeSeq.isEmpty) {
      throw new Exception("The element must have \"name\" attribute")
    }
    val name = nodeSeq.head.text  // String

    // validators is a WrappedArray
    // For smaller size, use Java array
    val jArray = validators.+:(name).toArray  // +: prepend
    val name2  = SecureBase64.serialize(jArray)

    val attr  = Attribute(None, "name", Text(name2), Null)
    val elem2 = elem % attr

    (elem2, name, name2)
  }

  //                                          name
  def takeOutFromName(name2: String): Option[(String, Iterable[Validator])] =
    SecureBase64.deserialize(name2) match {
      case None => None

      case Some(array) =>
        try {
          val array2 = array.asInstanceOf[Array[Any]]
          val name   = array2(0).asInstanceOf[String]

          val validators = new ArrayBuffer[Validator]
          for (i <- 1 until array2.length) {
            validators.append(array2(i).asInstanceOf[Validator])
          }
          Some((name, validators))
        } catch {
          case e => None
        }
    }
}

class ValidatorInjector(action: Action, elem: Elem) {
  import ValidatorInjector._

  def validate(validators: Validator*): Elem = {
    val (elem2, name, name2) = injectToName(elem, validators:_*)
    for (v <- validators) v.render(action, name, name2)
    elem2
  }
}
