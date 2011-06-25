package xitrum.validation

import scala.collection.mutable.{ArrayBuffer, HashMap => MHashMap}

import xitrum.Action
import xitrum.scope.Env

object ValidatorCaller {
  def call(action: Action): Boolean = {
    // Params in URL are not allowed for security because we only check bodyParams
    action.henv.uriParams.clear
    action.henv.pathParams.clear

    val (bodyParams, name_secureParamName_validators) = takeoutValidators(action.henv.bodyParams)

    action.henv.bodyParams.clear
    action.henv.bodyParams ++= bodyParams

    for ((paramName, secureParamName, validators) <- name_secureParamName_validators) {
      for (v <- validators) {
        if (!v.validate(action, paramName, secureParamName)) return false
      }
    }

    true
  }

  //----------------------------------------------------------------------------

  //                                                            decrypted params      paramName  secureParamName
  private def takeoutValidators(secureBodyParams: Env.Params): (Env.Params, Iterable[(String,    String, Iterable[Validator])]) = {
    val secureParamNames = secureBodyParams.keys

    val bodyParams2                          = new MHashMap[String, Array[String]]
    val paramName_secureParamName_validators = new ArrayBuffer[(String, String, Iterable[Validator])]
    for (secureParamName <- secureParamNames) {
      val value = secureBodyParams(secureParamName)

      ValidatorInjector.takeOutFromName(secureParamName) match {
        case None =>
          throw new Exception("Request contains invalid parameter name")

        case Some(paramName_validators) =>
          val (paramName, validators) = paramName_validators
          bodyParams2.put(paramName, value)
          paramName_secureParamName_validators.append((paramName, secureParamName, validators))
      }
    }
    (bodyParams2, paramName_secureParamName_validators)
  }
}
