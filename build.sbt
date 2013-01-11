organization := "tv.cntt"

name := "xitrum"

version := "1.15-SNAPSHOT"

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

// Hazelcast -------------------------------------------------------------------

// For distributed cache and Comet
// Infinispan is good but much heavier
libraryDependencies += "com.hazelcast" % "hazelcast" % "2.4.1"

// http://www.hazelcast.com/documentation.jsp#Clients
// Hazelcast can be configured in Xitrum as super client or native client
libraryDependencies += "com.hazelcast" % "hazelcast-client" % "2.4.1"

// Scalate ---------------------------------------------------------------------

libraryDependencies += "org.fusesource.scalate" %% "scalate-core" % "1.6.1"

// For Markdown
libraryDependencies += "org.fusesource.scalamd" %% "scalamd" % "1.6"

// For Scalate to compile CoffeeScript to JavaScript
libraryDependencies += "org.mozilla" % "rhino" % "1.7R4"

// Other dependencies ----------------------------------------------------------

libraryDependencies += "io.netty" % "netty" % "3.6.1.Final"

libraryDependencies += "org.javassist" % "javassist" % "3.17.1-GA"

// Projects using Xitrum must provide a concrete implentation of SLF4J (Logback etc.)
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.2" % "provided"

libraryDependencies += "org.jboss.marshalling" % "jboss-marshalling" % "1.3.16.GA"

libraryDependencies += "org.jboss.marshalling" % "jboss-marshalling-river" % "1.3.16.GA"

// For jsEscape
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.1.0"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.1.0"

libraryDependencies += "org.json4s" %% "json4s-native" % "3.1.0"

libraryDependencies += "tv.cntt" %% "scaposer" % "1.2"

libraryDependencies += "tv.cntt" %% "sclasner" % "1.2"

// xitrum.imperatively uses Scala continuation, a compiler plugin --------------

autoCompilerPlugins := true

addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.10.0")

scalacOptions += "-P:continuations:enable"

// http://www.scala-sbt.org/release/docs/Detailed-Topics/Cross-Build
//crossScalaVersions := Seq("2.10.0")
scalaVersion := "2.10.0"
