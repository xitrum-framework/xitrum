package xitrum.controller

import scala.util.Random

import io.netty.handler.codec.http.QueryStringEncoder

import xitrum.{Config, Controller}
import xitrum.etag.Etag
import xitrum.routing.{PostbackController, Route, Routes}
import xitrum.validator.ValidatorInjector

trait UrlFor {
  this: Controller =>

  /** @param path Relative to the "public" directory, without leading "/" */
  def urlForPublic(path: String) = {
    val absPath     = Config.root + "/public/" + path
    val forceReload = Etag.forFile(absPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.TooBig(file)                       => file.lastModified
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUri("/" + path + "?" + forceReload)
  }

  /** @param path Relative to an entry in classpath, without leading "/" */
  def urlForResource(path: String) = {
    val classPathPath = "public/" + path
    val forceReload = Etag.forResource(classPathPath, true) match {
      case Etag.NotFound                           => Random.nextLong.toString
      case Etag.Small(bytes, etag, mimeo, gzipped) => etag
    }
    Config.withBaseUri("/resources/public/" + path + "?" + forceReload)
  }

  //----------------------------------------------------------------------------

  def currentRoute = handlerEnv.route

  def postbackUrl(route: Route, extraParams: (String, Any)*): String = {
    val url = route.postbackUrl
    registerExtraParams(url, extraParams)
    url
  }

  def currentPostbackUrl(extraParams: (String, Any)*) = {
    postbackUrl(currentRoute, extraParams:_*)
  }

  private def registerExtraParams(url: String, extraParams: Iterable[(String, Any)]) {
    if (extraParams.isEmpty) return

    val e = new QueryStringEncoder("")
    extraParams.foreach { case (paramName, value) =>
      val secureParamName = ValidatorInjector.injectToParamName(paramName)
      e.addParam(secureParamName, value.toString)
    }

    val withLeadingQuestionMark    = e.toString  // ?p1=v1&p2=v2
    val withoutLeadingQuestionMark = withLeadingQuestionMark.substring(1)
    jsAddToView("$(\"[route='" + url + "']\").data(\"extra\", '" + withoutLeadingQuestionMark + "')")
  }
}