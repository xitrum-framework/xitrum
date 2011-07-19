organization := "my.organization"

name         := "my_project"

version      := "1.0-SNAPSHOT"

scalaVersion := "2.9.0-1"

scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked"
)

// For Xitrum
resolvers += "Sonatype Snapshot Repository" at "https://oss.sonatype.org/content/repositories/snapshots"

// For Jerkson, used by Xitrum
// https://github.com/codahale/jerkson
resolvers += "repo.codahale.com"            at "http://repo.codahale.com"

libraryDependencies += "tv.cntt"         %% "xitrum"          % "1.1-SNAPSHOT"

// Remove this when Netty 4 is released
libraryDependencies += "org.jboss.netty" %  "netty"           % "4.0.0.Alpha1-SNAPSHOT" from "https://hudson.jboss.org/jenkins/view/Netty/job/netty/lastSuccessfulBuild/artifact/target/netty-4.0.0.Alpha1-SNAPSHOT.jar"

// Xitrum uses SLF4J
// An implementation of SLF4J must be provided
libraryDependencies += "ch.qos.logback"  %  "logback-classic" % "0.9.29"

// For "sbt console"
unmanagedClasspath in Compile <+= (baseDirectory) map { bd => Attributed.blank(bd / "config") }

// For "sbt run"
unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "config") }
