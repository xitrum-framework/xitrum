package my_project.action

import xitrum.Action
import xitrum.comet.CometPublishAction
import xitrum.validation.Required
import xitrum.view.DocType

trait AppAction extends Action {
  override def layout = DocType.xhtmlTransitional(
    <html>
      <head>
        {xitrumHead}

        <meta content="text/html; charset=utf-8" http-equiv="content-type" />
        <title>Welcome to Xitrum</title>

        <link type="image/vnd.microsoft.icon" rel="shortcut icon" href={urlForPublic("../favicon.ico")} />

        <link type="text/css" rel="stylesheet" media="all" href={urlForPublic("css/960/reset.css")} />
        <link type="text/css" rel="stylesheet" media="all" href={urlForPublic("css/960/text.css")} />
        <link type="text/css" rel="stylesheet" media="all" href={urlForPublic("css/960/960.css")} />
        <link type="text/css" rel="stylesheet" media="all" href={urlForPublic("css/app.css")} />
      </head>
      <body>
        <div class="container_12">
          <h1><a href={urlFor[IndexAction]}>Welcome to Xitrum</a></h1>

          <div class="grid_8">
            <div id="flash">{jsFlash}</div>
            {renderedView}
          </div>

          <div class="grid_4">
            {renderChat}
          </div>
        </div>
        {jsForView}
      </body>
    </html>
  )

  private def renderChat = {
    jsCometGet("chat", """
      function(channel, timestamp, body) {
        var wasScrollAtBottom = xitrum.isScrollAtBottom('#chatOutput');

        var escaped = $('<div/>').text(body.chatInput[0]).html();
        $('#chatOutput').append('- ' + escaped + '<br />');

        if (wasScrollAtBottom) xitrum.scrollToBottom('#chatOutput');
      }
    """)

    <xml:group>
      <h3>Chat</h3>

      <div id="chatOutput"></div>

      <form postback="submit" action={urlForPostback[CometPublishAction]} after="function() { $('#chatInput').attr('value', '') }">
        <input type="hidden" name={validate("channel")} value="chat" />
        <input type="text" id="chatInput" name={validate("chatInput", Required)} />
      </form>
    </xml:group>
  }
}
