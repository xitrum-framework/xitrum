package xitrum.view

import io.netty.channel.ChannelFuture
import xitrum.Action

trait ViewResponder {
  this: Action =>

  def respondTemplate(uri: String, options: Map[String, Any]): ChannelFuture =
    respondText(renderTemplate(uri, options))

  def respondTemplate(uri: String): ChannelFuture =
    respondTemplate(uri, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondViewNoLayout(uri: String, options: Map[String, Any]): ChannelFuture =
    respondText(renderViewNoLayout(uri, options), "text/html")

  def respondViewNoLayout(uri: String): ChannelFuture =
    respondViewNoLayout(uri, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondViewNoLayout(actionClass: Class[_ <: Action], options: Map[String, Any]): ChannelFuture =
    respondText(renderViewNoLayout(actionClass, options), "text/html")

  def respondViewNoLayout(actionClass: Class[_ <: Action]): ChannelFuture =
    respondViewNoLayout(actionClass, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondViewNoLayout[T <: Action: Manifest](options: Map[String, Any]): ChannelFuture =
    respondViewNoLayout(getActionClass[T], options)

  def respondViewNoLayout[T <: Action: Manifest](): ChannelFuture =
    respondViewNoLayout(getActionClass[T], Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondView(customLayout: () => Any, uri: String, options: Map[String, Any]): ChannelFuture =
    respondText(renderView(customLayout, uri, options), "text/html")

  def respondView(customLayout: () => Any, uri: String): ChannelFuture =
    respondView(customLayout, uri, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondView(uri: String, options: Map[String, Any]): ChannelFuture =
    respondText(renderView(layout _, uri, options), "text/html")

  def respondView(uri: String): ChannelFuture =
    respondView(uri, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondView(customLayout: () => Any, actionClass: Class[_ <: Action], options: Map[String, Any]): ChannelFuture =
    respondText(renderView(customLayout, actionClass, options), "text/html")

  def respondView(customLayout: () => Any, actionClass: Class[_ <: Action]): ChannelFuture =
    respondView(customLayout, actionClass, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondView[T <: Action: Manifest](customLayout: () => Any, options: Map[String, Any]): ChannelFuture =
    respondView(customLayout, getActionClass[T], options)

  def respondView[T <: Action: Manifest](customLayout: () => Any): ChannelFuture =
    respondView(customLayout, getActionClass[T], Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondView(actionClass: Class[_ <: Action], options: Map[String, Any]): ChannelFuture =
    respondView(layout _, actionClass, options)

  def respondView(actionClass: Class[_ <: Action]): ChannelFuture =
    respondView(actionClass, Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondView[T <: Action: Manifest](options: Map[String, Any]): ChannelFuture =
    respondView(layout _, getActionClass[T], options)

  def respondView[T <: Action: Manifest](): ChannelFuture =
    respondView(layout _, getActionClass[T], Map.empty[String, Any])

  //----------------------------------------------------------------------------

  def respondInlineView(inlineView: Any): ChannelFuture =
    respondText(renderInlineView(inlineView), "text/html")
}
