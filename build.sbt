// To build Xitrum from source code:
// 1. From Xitrum source code directory, run SBT without any argument
// 2. From SBT prompt, run + publish-local (yes, with the plus sign)

organization := "tv.cntt"

name := "xitrum"

version := "1.7-SNAPSHOT"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

// Dependencies ----------------------------------------------------------------

// Use this when Netty 4 is released
//resolvers += "JBoss Repository" at "https://repository.jboss.org/nexus/content/groups/public/"
//"org.jboss.netty" % "netty" % "4"

// Remove this when Netty 4 is released
// The nightly build site of JBoss is not always online
// GitHub is more stable
libraryDependencies += "org.jboss.netty" % "netty" % "4.0.0.Alpha1-SNAPSHOT" from "http://cloud.github.com/downloads/ngocdaothanh/xitrum/netty-4.0.0.Alpha1-SNAPSHOT.jar"

// Projects using Xitrum must provide a concrete implentation of SLF4J (Logback etc.)
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.4" % "provided"

libraryDependencies += "tv.cntt" %% "sclasner" % "1.0"

libraryDependencies += "tv.cntt" %% "scaposer" % "1.0"

libraryDependencies += "org.javassist" % "javassist" % "3.15.0-GA"

// For distributed cache and Comet
// Infinispan is good but much heavier, and the logging is bad:
// https://github.com/infinispan/infinispan/blob/master/core/src/main/java/org/infinispan/util/logging/LogFactory.java
libraryDependencies += "com.hazelcast" % "hazelcast" % "1.9.4.4"

// http://www.hazelcast.com/documentation.jsp#Clients
// Hazelcast may be configured in Xitrum as super client or native client
libraryDependencies += "com.hazelcast" % "hazelcast-client" % "1.9.4.4"

// https://github.com/codahale/jerkson
// lift-json does not generate correctly for:
//   List(Map("user" -> List("langtu"), "body" -> List("hello world")))
resolvers += "repo.codahale.com" at "http://repo.codahale.com"

libraryDependencies += "com.codahale" %% "jerkson" % "0.5.0"

// For easier development (sbt console etc.)
unmanagedBase in Runtime <<= baseDirectory { base => base / "config" }

// Publish ---------------------------------------------------------------------

// https://github.com/harrah/xsbt/wiki/Cross-Build
//crossScalaVersions := Seq("2.9.0-1", "2.9.1")
scalaVersion := "2.9.1"

publishTo <<= (version) { version: String =>
  val nexus = "http://nexus.scala-tools.org/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "snapshots/")
  else                                   Some("releases"  at nexus + "releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// There is error with sbt doc
publishArtifact in (Compile, packageDoc) := false
