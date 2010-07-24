package xt.framework

import scala.collection.mutable.Stack

import org.fusesource.scalate.{AttributeMap, Template, TemplateEngine, RenderContext}
import org.fusesource.scalate.util.{RenderHelper, Logging}
import org.fusesource.scalate.support.{AttributesHashMap, Elvis}

import org.jboss.netty.buffer.{ChannelBuffer, ChannelBuffers}
import org.jboss.netty.util.CharsetUtil

/**
 * Implementation of RenderContext using Netty ChannelBuffer.
 */
class ChannelBufferRenderContext(val engine: TemplateEngine) extends RenderContext with Logging {
  val attributes: AttributeMap[String, Any] = new AttributesHashMap[String, Any]() {
    update("context", this)
  }
  escapeMarkup = engine.escapeMarkup

  /**
   * Provide access to the elvis operator so that we can use it to provide null handling nicely
   */
  implicit def anyToElvis(value: Any): Elvis = new Elvis(value)

  //----------------------------------------------------------------------------

  var out = ChannelBuffers.dynamicBuffer
  private val outStack = new Stack[ChannelBuffer]

  def <<(v: Any) {
    val s = value(v, false)
    out.writeBytes(s.getBytes)
  }

  def <<<(v: Any) {
    val s = value(v)
    out.writeBytes(s.getBytes)
  }

  /**
   * Evaluates the body capturing any output written to this page context during the body evaluation
   */
  def capture(body: => Unit): String = {
    outStack.push(out)
    out = ChannelBuffers.dynamicBuffer
    try {
      val u: Unit = body
      out.clear
      out.toString(CharsetUtil.UTF_8)
    } finally {
      out = outStack.pop
    }
  }

  /**
   * Evaluates the template capturing any output written to this page context during the body evaluation
   */
  def capture(template: Template): String = {
    outStack.push(out)
    out = ChannelBuffers.dynamicBuffer
    try {
      debug("Capturing template " + template)
      template.render(this)
      out.clear
      out.toString(CharsetUtil.UTF_8)
    } finally {
      out = outStack.pop
    }
  }
}
