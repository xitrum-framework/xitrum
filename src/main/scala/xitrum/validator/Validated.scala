package xitrum.validator

object Validated extends Validators(List()) {
  def secureParamName(paramName: String) = ValidatorInjector.injectToParamName(paramName)
}
