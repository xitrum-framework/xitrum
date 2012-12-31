organization := "tv.cntt"

name := "xitrum-xgettext"

version := "1.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

// http://www.scala-sbt.org/release/docs/Detailed-Topics/Cross-Build
//crossScalaVersions := Seq("2.10.0")
scalaVersion := "2.10.0"

libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.10.0"
