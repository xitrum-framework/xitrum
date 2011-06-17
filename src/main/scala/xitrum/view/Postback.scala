package xitrum.view

import scala.xml.{Attribute, Elem, Null, Text}
import xitrum.Action

class PostbackInjector(event: String, actionUrl: String) {
  def ::(elem: Elem) = {
    //cai input nao chua co validate thi them vao!

    val postback = Attribute(None, "postback", Text(event),     Null)
    val action   = Attribute(None, "action",   Text(actionUrl), Null)

    elem % postback % action
  }
}

trait Postback {
  this: Action =>

  def validate[T: Manifest](event: String) = {
    val actionUrl = urlForPostback[T]
    new PostbackInjector(event, actionUrl)
  }
}
