package xitrum.annotation

import scala.annotation.StaticAnnotation

sealed trait Cache extends StaticAnnotation

case class CacheActionDay   (days:    Int) extends Cache
case class CacheActionHour  (hours:   Int) extends Cache
case class CacheActionMinute(minutes: Int) extends Cache
case class CacheActionSecond(seconds: Int) extends Cache

case class CachePageDay   (days:    Int) extends Cache
case class CachePageHour  (hours:   Int) extends Cache
case class CachePageMinute(minutes: Int) extends Cache
case class CachePageSecond(seconds: Int) extends Cache
