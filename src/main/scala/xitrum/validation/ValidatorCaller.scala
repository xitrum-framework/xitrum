package xitrum.validation

import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap}

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
    val securedParamNames = securedBodyParams.keys

    val bodyParams2                           = new MHashMap[String, Array[String]]
    val paramName_securedParamName_validators = new ArrayBuffer[(String, String, Iterable[Validator])]
    for (securedParamName <- securedParamNames) {
      val value = securedBodyParams(securedParamName)

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
