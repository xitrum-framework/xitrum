package colinh.view.layouts

import xt.framework.CV

class Application extends CV {
  def render = {
    <html>
      <head>
        <title>{at("title")}</title>

        <link rel="shortcut icon" href="/static/img/favicon.ico" />
        <link rel="stylesheet" href="/static/css/960/reset.css" />
        <link rel="stylesheet" href="/static/css/960/text.css" />
        <link rel="stylesheet" href="/static/css/960/960.css" />
        <link rel="stylesheet" href="/static/css/application.css" />

        <script type="text/javascript" src="/static/js/jquery-1.4.2.min.js"></script>
      </head>

      <body>
        <div class="container_12">
          <div id="header">
            <h1>Colinh</h1>
          </div>

          <div id="content">{at("content-for-layout")}</div>

          <div id="footer">Powered by Colinh &amp; Xitrum</div>
        </div>
      </body>
    </html>
  }
}
