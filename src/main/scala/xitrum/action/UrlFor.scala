package xitrum.action

import scala.util.Random

import org.jboss.netty.handler.codec.http.QueryStringEncoder

import xitrum.{Action, Config}
import xitrum.etag.Etag
import xitrum.routing.{PostbackAction, Routes}
import xitrum.util.SecureBase64
import xitrum.validation.ValidatorInjector

trait UrlFor {
  this: Action =>

  def urlFor[T: Manifest](params: (String, Any)*) = {
    val actionClass = manifest[T].erasure.asInstanceOf[Class[Action]]
    Routes.urlFor(actionClass, params:_*)
  }
  def absoluteUrlFor[T: Manifest](params: (String, Any)*) = absoluteUrlPrefix + urlFor[T](params:_*)

  /**
   * When there are no params, the application developer can write
   * urlFor[MyAction], instead of urlFor[MyAction]().
   */
  def urlFor[T: Manifest]: String = urlFor[T]()
  def absoluteUrlFor[T: Manifest]: String = absoluteUrlPrefix + urlFor[T]()

  def urlForThis(params: (String, Any)*) = Routes.urlFor(this.getClass.asInstanceOf[Class[Action]], params:_*)
  def absoluteUrlForThis(params: (String, Any)*) = absoluteUrlPrefix + urlForThis(params:_*)

  def urlForThis: String = urlForThis()
  def absoluteUrlForThis = absoluteUrlPrefix + urlForThis

  //----------------------------------------------------------------------------

  def urlForPostback[T: Manifest](extraParams: (String, Any)*): String = {
    val actionClass = manifest[T].erasure.asInstanceOf[Class[Action]]
    val url = urlForPostbackAction(actionClass)
    registerExtraParams(url, extraParams)
    url
  }

  def urlForPostbackThis(extraParams: (String, Any)*) = {
    val actionClass = this.getClass.asInstanceOf[Class[Action]]
    val url = urlForPostbackAction(actionClass)
    registerExtraParams(url, extraParams)
    url
  }

  def urlForPostback[T: Manifest]: String = urlForPostback[T]()
  def urlForPostbackThis: String          = urlForPostbackThis()

  private def urlForPostbackAction(actionClass: Class[Action]): String = {
    val className       = actionClass.getName
    val secureClassName = SecureBase64.encrypt(className)
    val url = PostbackAction.POSTBACK_PREFIX + secureClassName
    Config.withBaseUri(url)
  }

  //----------------------------------------------------------------------------

  /** path must be relative to the "public" directory */
  def urlForPublic(path: String) = {
    val absPath     = Config.root + "/public/" + path
    val forceReload = Etag.forFile(absPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.TooBig(file)                       => file.lastModified
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUri(path + "?" + forceReload)
  }

  def urlForResource(path: String) = {
    val classPathPath = "public/" + path
    val forceReload = Etag.forResource(classPathPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUri("/resources/public/" + path + "?" + forceReload)
  }

  //----------------------------------------------------------------------------

  private def registerExtraParams(url: String, extraParams: Iterable[(String, Any)]) {
    if (extraParams.isEmpty) return

    val e = new QueryStringEncoder("")
    extraParams.foreach { case (paramName, value) =>
      val secureParamName = ValidatorInjector.injectToParamName(paramName)
      e.addParam(secureParamName, value.toString)
    }

    val withLeadingQuestionMark    = e.toString  // ?p1=v1&p2=v2
    val withoutLeadingQuestionMark = withLeadingQuestionMark.substring(1)
    jsAddToView("$(\"[action='" + url + "']\").data(\"extra\", '" + withoutLeadingQuestionMark + "')")
  }
}
