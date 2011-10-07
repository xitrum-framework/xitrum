package xitrum.validation

object Validated extends Validators(List()) {
  def secureParamName(paramName: String) = ValidatorInjector.injectToParamName(paramName)
}
