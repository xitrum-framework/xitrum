organization := "tv.cntt"

name := "xitrum-xgettext"

version := "1.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

// https://github.com/harrah/xsbt/wiki/Cross-Build
//crossScalaVersions := Seq("2.9.1", "2.9.2")
scalaVersion := "2.9.2"

libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.9.2"

// Copy dev/build.sbt.end here when publishing
