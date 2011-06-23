package my_project.action

import xitrum.Action
import xitrum.view.DocType

trait AppAction extends Action {
  override def layout = DocType.xhtmlTransitional(
    <html>
      <head>
        {xitrumHead}
        <title>Welcome to Xitrum</title>
      </head>
      <body>
        {renderedView}
      </body>
    </html>
  )
}
