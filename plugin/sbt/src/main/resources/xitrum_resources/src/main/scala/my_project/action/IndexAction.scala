package my_project.action

import xitrum.Action
import xitrum.annotation.GET

@GET("/")
class IndexAction extends AppAction {
  override def execute {
    renderView(
      <p>
        Xitrum is open source:
        <ul>
          <li>Source code: <a href="https://github.com/ngocdaothanh/xitrum">https://github.com/ngocdaothanh/xitrum</a></li>
          <li>Documentation: <a href="http://ngocdaothanh.github.com/xitrum">http://ngocdaothanh.github.com/xitrum</a></li>
          <li>Google group: <a href="http://groups.google.com/group/xitrum-framework">http://groups.google.com/group/xitrum-framework</a></li>
        </ul>
      </p>)
  }
}
