sbtPlugin := true

organization := "tv.cntt"

name := "xitrum-plugin"

version := "1.4-SNAPSHOT"

// Publish ---------------------------------------------------------------------

// https://github.com/harrah/xsbt/wiki/Cross-Build
// SBT 0.11.0 uses Scala 2.9.1
crossScalaVersions := Seq("2.9.1")

publishTo <<= (version) { version: String =>
  val nexus = "http://nexus.scala-tools.org/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/")
  else                                   Some("releases"  at nexus + "releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
