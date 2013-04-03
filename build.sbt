organization := "tv.cntt"

name := "xitrum"

version := "1.21-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked"
)

// Put config directory in classpath for easier development (sbt console etc.)
unmanagedBase in Runtime <<= baseDirectory { base => base / "config" }

// Most Scala projects are published to Sonatype, but Sonatype is not default
// and it takes several hours to sync from Sonatype to Maven Central
resolvers += "SonatypeReleases" at "http://oss.sonatype.org/content/repositories/releases/"

libraryDependencies += "io.netty" % "netty" % "3.6.3.Final"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.2"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.1.2"

// Hazelcast is used for distributed cache and SockJS
// Infinispan is good but much heavier
libraryDependencies += "com.hazelcast" % "hazelcast" % "2.5"

// Hazelcast can be configured as cluster member, lite member, or Java client
libraryDependencies += "com.hazelcast" % "hazelcast-client" % "2.5"

libraryDependencies += "org.javassist" % "javassist" % "3.17.1-GA"

// Projects using Xitrum must provide a concrete implentation of SLF4J (Logback etc.)
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.3" % "provided"

libraryDependencies += "com.twitter" %% "chill" % "0.2.0"

// For jsEscape
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.2.2"

libraryDependencies += "tv.cntt" %% "scaposer" % "1.2"

libraryDependencies += "tv.cntt" %% "sclasner" % "1.2"

// xitrum.imperatively uses Scala continuation, a compiler plugin --------------

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.1")

scalacOptions += "-P:continuations:enable"
