// To build Xitrum from source code:
// 1. From Xitrum source code directory, run SBT without any argument
// 2. From SBT prompt, run + publish-local (yes, with the plus sign)

organization := "tv.cntt"

name := "xitrum"

version := "1.1-SNAPSHOT"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

// Dependencies ----------------------------------------------------------------

// Use this when Netty 4 is released
//resolvers += "JBoss Repository" at "https://repository.jboss.org/nexus/content/groups/public/"
//"org.jboss.netty" % "netty" % "3.2.4.Final"

// Remove this when Netty 4 is released
libraryDependencies += "org.jboss.netty" % "netty" % "4.0.0.Alpha1-SNAPSHOT" from "https://hudson.jboss.org/jenkins/view/Netty/job/netty/lastSuccessfulBuild/artifact/target/netty-4.0.0.Alpha1-SNAPSHOT.jar"

// Projects using Xitrum must provide a concrete implentation of SLF4J (Logback etc.)
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.1" % "provided"

libraryDependencies += "tv.cntt" % "annovention" % "1.0"

// For distributed cache and Comet
// Infinispan is good but much heavier, and the logging is bad:
// https://github.com/infinispan/infinispan/blob/master/core/src/main/java/org/infinispan/util/logging/LogFactory.java
libraryDependencies += "com.hazelcast" % "hazelcast" % "1.9.3.1"

// http://www.hazelcast.com/documentation.jsp#Clients
// Hazelcast may be configured in Xitrum as super client or native client
libraryDependencies += "com.hazelcast" % "hazelcast-client" % "1.9.3.1"

// https://github.com/codahale/jerkson
// lift-json does not generate correctly for:
//   List(Map("user" -> List("langtu"), "body" -> List("hello world")))
resolvers += "repo.codahale.com" at "http://repo.codahale.com"

libraryDependencies += "com.codahale" %% "jerkson" % "0.4.0"

// For easier development (sbt console etc.)
unmanagedBase in Runtime <<= baseDirectory { base => base / "config" }

// Publish ---------------------------------------------------------------------

// https://github.com/harrah/xsbt/wiki/Cross-Build
// Lastest version of 2.8.x is 2.8.1, of 2.9.x is 2.9.0-1
crossScalaVersions := Seq("2.8.1", "2.9.0-1")

publishTo := Some("Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots")

credentials += Credentials(new File(System.getProperty("user.home") + "/.ivy2/.credentials"))

// There is error with sbt doc
publishArtifact in (Compile, packageDoc) := false
