package xitrum.validator

import scala.collection.mutable.{ArrayBuffer, Map => MMap}

import xitrum.Controller
import xitrum.scope.request.Params

object ValidatorCaller {
  def call(controller: Controller): Boolean = {
    // Params in URL are not allowed for security, we only check bodyParams
    controller.handlerEnv.uriParams.clear()
    controller.handlerEnv.pathParams.clear()

    val (bodyParams, paramName_secureParamName_validators) = takeoutValidators(controller.handlerEnv.bodyParams)

    controller.handlerEnv.bodyParams.clear()
    controller.handlerEnv.bodyParams ++= bodyParams

    // See note at textParams
    controller.textParams.clear()
    controller.textParams ++= bodyParams

    for ((paramName, secureParamName, validators) <- paramName_secureParamName_validators) {
      for (v <- validators) {
        if (!v.validate(controller, paramName, secureParamName)) return false
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
