package xitrum.validation

import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import xitrum.Action
import xitrum.scope.request.Params

object ValidatorCaller {
  def call(action: Action): Boolean = {
    // Params in URL are not allowed for security because we only check bodyParams
    action.handlerEnv.uriParams.clear
    action.handlerEnv.pathParams.clear

    val (bodyParams, name_secureParamName_validators) = takeoutValidators(action.handlerEnv.bodyParams)

    action.handlerEnv.bodyParams.clear
    action.handlerEnv.bodyParams ++= bodyParams

    for ((paramName, secureParamName, validators) <- name_secureParamName_validators) {
      for (v <- validators) {
        if (!v.validate(action, paramName, secureParamName)) return false
      }
    }

    true
  }

  //----------------------------------------------------------------------------

  //                                                       decrypted params   paramName  secureParamName
  private def takeoutValidators(secureBodyParams: Params): (Params, Iterable[(String,    String, Iterable[Validator])]) = {
    val secureParamNames = secureBodyParams.keys

    val bodyParams2                          = MMap[String, List[String]]()
    val paramName_secureParamName_validators = ArrayBuffer[(String, String, Iterable[Validator])]()
    for (secureParamName <- secureParamNames) {
      val params = secureBodyParams(secureParamName)

      ValidatorInjector.takeOutFromName(secureParamName) match {
        case None =>
          throw new Exception("Request contains invalid parameter name")

        case Some(paramName_validators) =>
          val (paramName, validators) = paramName_validators

          if (bodyParams2.contains(paramName)) {
            val array = bodyParams2(paramName)
            bodyParams2.put(paramName, array ++ params)
          } else
            bodyParams2.put(paramName, params)

          paramName_secureParamName_validators.append((paramName, secureParamName, validators))
      }
    }
    (bodyParams2, paramName_secureParamName_validators)
  }
}
