package my_project.action

import xitrum.Action
import xitrum.annotation.GET

@GET("/")
class IndexAction extends AppAction {
  override def execute {
    renderView(<p>Hello World</p>)
  }
}
