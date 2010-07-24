package colinh.view.layouts

import scala.xml.NodeSeq
import xt.framework.View

class Application extends View {
  def render = {
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
      <head>
        <title>{at("title")}</title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />

        <link type="image/vnd.microsoft.icon" rel="shortcut icon" href="/static/img/favicon.ico" />

        <link type="text/css" rel="stylesheet" media="all" href="/static/css/960/reset.css" />
        <link type="text/css" rel="stylesheet" media="all" href="/static/css/960/text.css" />
        <link type="text/css" rel="stylesheet" media="all" href="/static/css/960/960.css" />
        <link type="text/css" rel="stylesheet" media="all" href="/static/css/application.css" />

        <script type="text/javascript" src="/static/js/jquery-1.4.2.min.js"></script>
      </head>

      <body>
        <div class="container_12">
          <div id="header">
            <h1>Colinh</h1>
          </div>

          <div id="content" class="grid_9">
            {at[NodeSeq]("content_for_layout")}
          </div>

          <div id="sidebar" class="grid_3">
            Sidebar goes here
          </div>

          <div class="clear"></div>

          <div id="footer">Powered by Colinh &amp; Xitrum</div>
        </div>
      </body>
    </html>
  }
}
