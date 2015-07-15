package xitrum.view

import scala.xml.{Node, Xhtml}

object DocType {
  // See Lift

  def html5(html: Node)             = attachDocType("<!DOCTYPE html>", html)
  def xhtml11(html: Node)           = attachDocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">", html)
  def xhtmlFrameset(html: Node)     = attachDocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">", html)
  def xhtmlMobile(html: Node)       = attachDocType("<!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.0//EN\" \"http://www.wapforum.org/DTD/xhtml-mobile10.dtd\">", html)
  def xhtmlStrict(html: Node)       = attachDocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">", html)
  def xhtmlTransitional(html: Node) = attachDocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">", html)

  private def attachDocType(docType: String, html: Node): String = {
    // <br />.toString will create <br></br> which renders as 2 <br /> on some browsers!
    // http://www.scala-lang.org/node/492
    // http://www.ne.jp/asahi/hishidama/home/tech/scala/xml.html
    docType + "\n" + Xhtml.toXhtml(html)
  }
}
