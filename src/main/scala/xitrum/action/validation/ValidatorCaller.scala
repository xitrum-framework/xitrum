package xitrum.action.validation

import java.util.{ArrayList, List => JList, HashMap => JHashMap}
import scala.collection.mutable.ArrayBuffer

import xitrum.action.Action
import xitrum.action.env.Env

object ValidatorCaller {
  def call(action: Action): Boolean = {
    // bodyParams:  The name of parameters has been encrypted
    // bodyParams2: The name of parameters has been decrypted to normal, normal as the application developers see them
    val securedBodyParams = action.bodyParams
    val (bodyParams, name_securedParamName_validators) = takeoutValidators(securedBodyParams)
    action.bodyParams = bodyParams

    for ((paramName, securedParamName, validators) <- name_securedParamName_validators) {
      for (v <- validators) {
        if (!v.validate(action, paramName, securedParamName)) return false
      }
    }

    true
  }

  //----------------------------------------------------------------------------

  //                                                             decrypted params      paramName  securedParamName
  private def takeoutValidators(securedBodyParams: Env.Params): (Env.Params, Iterable[(String,    String, Iterable[Validator])]) = {
    val securedParamNames = securedBodyParams.keySet
    val i                 = securedParamNames.iterator

    val bodyParams2                           = new JHashMap[String, JList[String]]
    val paramName_securedParamName_validators = new ArrayBuffer[(String, String, Iterable[Validator])]
    while (i.hasNext) {
      val securedParamName = i.next
      val value            = securedBodyParams.get(securedParamName).asInstanceOf[JList[String]]

      ValidatorInjector.takeOutFromName(securedParamName) match {
        case None =>
          throw new Exception("Request contains invalid parameter name")

        case Some(paramName_validators) =>
          val (paramName, validators) = paramName_validators
          bodyParams2.put(paramName, value)
          paramName_securedParamName_validators.append((paramName, securedParamName, validators))
      }
    }
    (bodyParams2, paramName_securedParamName_validators)
  }
}
