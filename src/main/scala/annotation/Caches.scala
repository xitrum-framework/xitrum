package xitrum.annotation

import scala.annotation.StaticAnnotation

sealed trait CacheAnnotation extends StaticAnnotation

case class CacheActionDay   (days:    Int) extends CacheAnnotation
case class CacheActionHour  (hours:   Int) extends CacheAnnotation
case class CacheActionMinute(minutes: Int) extends CacheAnnotation
case class CacheActionSecond(seconds: Int) extends CacheAnnotation

case class CachePageDay   (hours:   Int) extends CacheAnnotation
case class CachePageHour  (days:    Int) extends CacheAnnotation
case class CachePageMinute(minutes: Int) extends CacheAnnotation
case class CachePageSecond(seconds: Int) extends CacheAnnotation
