package xitrum.view

import scala.xml.Elem

/** See Lift */
object DocType {
  def html5(html: Elem)             = attachDocType("<!DOCTYPE html>", html)
  def xhtml11(html: Elem)           = attachDocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">", html)
  def xhtmlFrameset(html: Elem)     = attachDocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">", html)
  def xhtmlMobile(html: Elem)       = attachDocType("<!DOCTYPE html PUBLIC \"-//WAPFORUM//DTD XHTML Mobile 1.0//EN\" \"http://www.wapforum.org/DTD/xhtml-mobile10.dtd\">", html)
  def xhtmlStrict(html: Elem)       = attachDocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">", html)
  def xhtmlTransitional(html: Elem) = attachDocType("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">", html)

  private def attachDocType(docType: String, html: Elem): String = {
    docType + "\n" + html
  }
}
