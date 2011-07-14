sbtPlugin := true

organization := "tv.cntt"

name := "xitrum-plugin"

version := "1.1-SNAPSHOT"

// Publish ---------------------------------------------------------------------

// https://github.com/harrah/xsbt/wiki/Cross-Build
// Lastest version of 2.8.x is 2.8.1, of 2.9.x is 2.9.0-1
//
// SBT 0.10.0 uses Scala 2.8.1
//crossScalaVersions := Seq("2.8.1", "2.9.0-1")
crossScalaVersions := Seq("2.8.1")

publishTo := Some("Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots")

credentials += Credentials(new File(System.getProperty("user.home") + "/.ivy2/.credentials"))
