package xt.middleware

import org.jboss.netty.channel.Channel
import org.jboss.netty.handler.codec.http.{HttpRequest, HttpResponse}

trait App {
  def call(channel: Channel, request: HttpRequest, response: HttpResponse, env: Env)
}
