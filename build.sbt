organization := "tv.cntt"

name := "xitrum"

version := "2.5-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

// http://www.scala-sbt.org/release/docs/Detailed-Topics/Java-Sources
// Avoid problem when Xitrum is built with Java 7 but the projects that use Xitrum
// are run with Java 6
// java.lang.UnsupportedClassVersionError: xitrum/annotation/First : Unsupported major.minor version 51.0
javacOptions ++= Seq(
  "-source",
  "1.6"
)

// Put config directory in classpath for easier development (sbt console etc.)
unmanagedBase in Runtime <<= baseDirectory { base => base / "config" }

// Most Scala projects are published to Sonatype, but Sonatype is not default
// and it takes several hours to sync from Sonatype to Maven Central
resolvers += "SonatypeReleases" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "io.netty" % "netty" % "3.6.5.Final"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.1.2"

// Projects using Xitrum must provide a concrete implentation of SLF4J (Logback etc.)
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.5" % "provided"

// For scanning routes
libraryDependencies += "tv.cntt" %% "sclasner" % "1.6"

// For (de)serializing
libraryDependencies += "com.twitter" %% "chill" % "0.2.1"

// For jsEscape
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.4"

// For i18n
libraryDependencies += "tv.cntt" %% "scaposer" % "1.2"

// For compiling CoffeeScript to JavaScript
libraryDependencies += "tv.cntt" % "rhinocoffeescript" % "1.6.2"

// Hazelcast is used for distributed cache and SockJS --------------------------

// Infinispan is good but much heavier
libraryDependencies += "com.hazelcast" % "hazelcast" % "2.5.1"

// Hazelcast can be configured as cluster member, lite member, or Java client
libraryDependencies += "com.hazelcast" % "hazelcast-client" % "2.5.1"

// xitrum.imperatively uses Scala continuation, a compiler plugin --------------

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.1")

scalacOptions += "-P:continuations:enable"

//------------------------------------------------------------------------------

// Skip API doc generation to speedup "publish-local" while developing.
// Comment out this line when publishing to Sonatype.
publishArtifact in (Compile, packageDoc) := false
