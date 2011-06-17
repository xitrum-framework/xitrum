package my_package

import xitrum.action.Action
import xitrum.action.annotation.GET

@GET("/")
class IndexAction extends Action {
  override def execute {
    renderView(<p>Hello World</p>)
  }
}
