organization := "tv.cntt"

name := "xitrum-xgettext"

version := "1.1-SNAPSHOT"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

libraryDependencies += "org.scala-lang" % "scala-compiler" % "2.9.1"

// Publish ---------------------------------------------------------------------

// https://github.com/harrah/xsbt/wiki/Cross-Build
// Lastest version of 2.8.x is 2.8.1, of 2.9.x is 2.9.0-1
//
// SBT 0.10.0 uses Scala 2.8.1
//crossScalaVersions := Seq("2.9.0-1", "2.9.1")
crossScalaVersions := Seq("2.9.1")

publishTo <<= (version) { version: String =>
  val nexus = "http://nexus.scala-tools.org/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/")
  else                                   Some("releases"  at nexus + "releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
