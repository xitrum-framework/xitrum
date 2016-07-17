package xitrum.view

import xitrum.Action

trait Renderer {
  this: Action =>

  def renderEventSource(data: Any, event: String = "message"): String = {
    val builder = new StringBuilder

    if (event != "message") {
      builder.append("event: ")
      builder.append(event)
      builder.append("\n")
    }

    val lines = data.toString.split("\n")
    val n = lines.length
    for (i <- 0 until n) {
      if (i < n - 1) {
        builder.append("data: ")
        builder.append(lines(i))
        builder.append("\n")
      } else {
        builder.append("data: ")
        builder.append(lines(i))
      }
    }

    builder.append("\r\n\r\n")
    builder.toString
  }
}
