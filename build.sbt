organization := "tv.cntt"

name := "xitrum"

version := "1.9.8-SNAPSHOT"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

// Put config directory in classpath for easier development (sbt console etc.)
unmanagedBase in Runtime <<= baseDirectory { base => base / "config" }

// Akka ------------------------------------------------------------------------

resolvers += "Typesafe" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" % "akka-actor" % "2.0.3"

// Hazelcast -------------------------------------------------------------------

// For distributed cache and Comet
// Infinispan is good but much heavier
libraryDependencies += "com.hazelcast" % "hazelcast" % "2.3.1"

// http://www.hazelcast.com/documentation.jsp#Clients
// Hazelcast can be configured in Xitrum as super client or native client
libraryDependencies += "com.hazelcast" % "hazelcast-client" % "2.3.1"

// Jerkson ---------------------------------------------------------------------

// https://github.com/codahale/jerkson
// lift-json does not generate correctly for:
//   List(Map("user" -> List("langtu"), "body" -> List("hello world")))
resolvers += "repo.codahale.com" at "http://repo.codahale.com"

libraryDependencies += "com.codahale" % "jerkson_2.9.1" % "0.5.0"

// Scalate ---------------------------------------------------------------------

libraryDependencies += "org.fusesource.scalate" % "scalate-core" % "1.5.3"

// For Scalate to compile CoffeeScript to JavaScript
libraryDependencies += "org.mozilla" % "rhino" % "1.7R4"

// JBoss Serialization ---------------------------------------------------------

libraryDependencies += "jboss" % "jboss-serialization" % "4.2.2.GA"

libraryDependencies += "net.sf.trove4j" % "trove4j" % "3.0.3"

libraryDependencies += "org.slf4j" % "log4j-over-slf4j" % "1.7.2"

// Other dependencies ----------------------------------------------------------

libraryDependencies += "io.netty" % "netty" % "3.5.8.Final"

libraryDependencies += "org.javassist" % "javassist" % "3.16.1-GA"

// Projects using Xitrum must provide a concrete implentation of SLF4J (Logback etc.)
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.2" % "provided"

libraryDependencies += "tv.cntt" %% "scaposer" % "1.1"

libraryDependencies += "tv.cntt" %% "sclasner" % "1.1"

// xitrum.imperatively uses Scala continuation, a compiler plugin --------------

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.2")

scalacOptions += "-P:continuations:enable"

// https://github.com/harrah/xsbt/wiki/Cross-Build
//crossScalaVersions := Seq("2.9.1", "2.9.2")
scalaVersion := "2.9.2"

// Copy dev/build.sbt.end here when publishing to Sonatype
