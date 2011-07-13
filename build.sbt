organization := "tv.cntt"

name := "xitrum"

version := "1.1-SNAPSHOT"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

// Dependencies ----------------------------------------------------------------

// Log using SLF4J
// Projects using Xitrum must provide a concrete implentation (Logback etc.).
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.1" % "provided"

// Web server

// Use this when Netty 4 is released
//resolvers += "JBoss Repository" at "https://repository.jboss.org/nexus/content/groups/public/"
//"org.jboss.netty" % "netty" % "3.2.4.Final"

resolvers += "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository"

libraryDependencies += "org.jboss.netty" % "netty" % "4.0.0.Alpha1-SNAPSHOT"

// For scanning all Controllers to build routes
resolvers += "Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "tv.cntt" % "annovention" % "1.0-SNAPSHOT"

// For distributed cache and Comet
// Infinispan is good but much heavier, and the logging is bad:
// https://github.com/infinispan/infinispan/blob/master/core/src/main/java/org/infinispan/util/logging/LogFactory.java
libraryDependencies += "com.hazelcast" % "hazelcast" % "1.9.3"

// https://github.com/lift/framework/tree/2.4-M1-release/core/json
libraryDependencies += "net.liftweb" %% "lift-json" % "2.4-M1"

// For easier development (sbt console etc.)
unmanagedBase in Runtime <<= baseDirectory { base => base / "config" }

// Publish ---------------------------------------------------------------------

// https://github.com/harrah/xsbt/wiki/Cross-Build
// Lastest version of 2.8.x is 2.8.1, of 2.9.x is 2.9.0-1
crossScalaVersions := Seq("2.8.1", "2.9.0-1")

publishTo := Some("Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// There is error with sbt doc
publishArtifact in (Compile, packageDoc) := false
