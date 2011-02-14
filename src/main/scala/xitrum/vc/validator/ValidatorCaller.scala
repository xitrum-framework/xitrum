package xitrum.vc.validator

import java.util.{ArrayList, List => JList, HashMap => JHashMap}
import scala.collection.mutable.ArrayBuffer

import xitrum.Action
import xitrum.vc.env.Env

object ValidatorCaller {
  def call(action: Action): Boolean = {
    val bodyParams = action.bodyParams
    val (bodyParams2, name_name2_validatorsColl) = takeoutValidators(bodyParams)
    action.bodyParams = bodyParams2

    for ((name, name2, validators) <- name_name2_validatorsColl) {
      for (v <- validators) v.validate(action, name, name2)
    }

    true
  }

  //----------------------------------------------------------------------------

  private def takeoutValidators(bodyParams: Env.Params): (Env.Params, Iterable[(String, String, Iterable[Validator])]) = {
    val name2s = bodyParams.keySet
    val i      = name2s.iterator

    val bodyParams2               = new JHashMap[String, JList[String]]
    val name_name2_validatorsColl = new ArrayBuffer[(String, String, Iterable[Validator])]
    while (i.hasNext) {
      val name2 = i.next
      val value = bodyParams.get(name2).asInstanceOf[JList[String]]

      ValidatorInjector.takeOutFromName(name2) match {
        case None =>
          bodyParams2.put(name2, value)

        case Some(name_validators) =>
          val (name, validators) = name_validators
          bodyParams2.put(name, value)
          name_name2_validatorsColl.append((name, name2, validators))
      }
    }
    (bodyParams2, name_name2_validatorsColl)
  }
}
