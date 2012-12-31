organization := "tv.cntt"

name := "xitrum-xgettext"

version := "1.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

// https://github.com/harrah/xsbt/wiki/Cross-Build
scalaVersion := "2.10.0"

libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.10.0"
