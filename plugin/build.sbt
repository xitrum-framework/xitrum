sbtPlugin := true

organization := "tv.cntt"

name := "xitrum-plugin"

version := "1.1-SNAPSHOT"

// Publish ---------------------------------------------------------------------

publishTo := Some("Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
