package xt.framework

import scala.collection.mutable.{Map, ListBuffer}
import scala.xml.Elem

trait View extends Helper {
  def render: Elem
}
