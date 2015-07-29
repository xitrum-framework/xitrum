organization := "tv.cntt"
name         := "xitrum"

scalaVersion       := "2.11.6"  // "2.10.5"
crossScalaVersions := Seq("2.11.6", "2.10.5")

// Run sbt mima-report-binary-issues to check for binary compatibility ---------
// https://github.com/typesafehub/migration-manager
version := "3.25.0-SNAPSHOT"
//com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
//com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact := Some("tv.cntt" % ("xitrum_" + scalaBinaryVersion.value) % "3.25.0")

//------------------------------------------------------------------------------

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// xitrum.util.FileMonitor requires Java 7
javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

// Source code for Scala 2.10 and 2.11 are a little different ------------------
// See src/main/scala-2.10 and src/main/scala-2.11.

unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "main" / s"scala-${scalaBinaryVersion.value}"

//------------------------------------------------------------------------------

// Projects using Xitrum must provide a concrete implementation of SLF4J (Logback etc.)
libraryDependencies += "org.slf4s" %% "slf4s-api" % "1.7.12"

// Netty is the core of Xitrum's HTTP(S) feature
libraryDependencies += "io.netty" % "netty-all" % "4.0.30.Final"

// https://github.com/netty/netty/wiki/Forked-Tomcat-Native
// https://groups.google.com/forum/#!topic/netty/oRATC6Tl0A4
// Include all classifiers for convenience
libraryDependencies += "io.netty" % "netty-tcnative" % "1.1.33.Fork4" classifier "linux-x86_64"
libraryDependencies += "io.netty" % "netty-tcnative" % "1.1.33.Fork4" classifier "osx-x86_64"
libraryDependencies += "io.netty" % "netty-tcnative" % "1.1.33.Fork4" classifier "windows-x86_64"

// https://github.com/netty/netty/wiki/Native-transports
// Only works on Linux
libraryDependencies += "io.netty" % "netty-transport-native-epoll" % "4.0.30.Final" classifier "linux-x86_64"

// Javassist boosts Netty 4 speed
libraryDependencies += "org.javassist" % "javassist" % "3.20.0-GA"

// Redirect Akka log to SLF4J
libraryDependencies += "com.typesafe.akka" %% "akka-actor"   % "2.3.12"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.3.12"
libraryDependencies += "com.typesafe.akka" %% "akka-contrib" % "2.3.12"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j"   % "2.3.12"

// For clustering SockJS with Akka
libraryDependencies += "tv.cntt" %% "glokka" % "2.3"

// For file watch
// (akka-agent is added here, should ensure same Akka version as above)
libraryDependencies += "com.beachape.filemanagement" %% "schwatcher" % "0.1.8"

// For scanning routes
libraryDependencies += "tv.cntt" %% "sclasner" % "1.7.0"

// For binary (de)serializing
libraryDependencies += "com.twitter" %% "chill" % "0.6.0"

// For JSON (de)serializing
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.11"

// For i18n
libraryDependencies += "tv.cntt" %% "scaposer" % "1.5"

// For jsEscape
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4"

// For compiling CoffeeScript to JavaScript
libraryDependencies += "tv.cntt" % "rhinocoffeescript" % "1.9.2"

// For metrics
libraryDependencies += "nl.grons" %% "metrics-scala" % "3.5.1_a2.3"

// For metrics
libraryDependencies += "io.dropwizard.metrics" % "metrics-json" % "3.1.2"

// JSON4S uses scalap 2.10.0/2.11.0, which in turn uses scala-compiler 2.10.0/2.11.0, which in
// turn uses scala-reflect 2.10.0/2.11.0. We need to force "scalaVersion" above, because
// Scala annotations (used by routes and Swagger) compiled by a newer version
// can't be read by an older version.
//
// Also, we must release a new version of Xitrum every time a new version of
// Scala is released.
libraryDependencies <+= scalaVersion { sv => "org.scala-lang" % "scalap" % sv }

// WebJars ---------------------------------------------------------------------

libraryDependencies += "org.webjars" % "jquery" % "2.1.4"

libraryDependencies += "org.webjars" % "jquery-validation" % "1.14.0"

libraryDependencies += "org.webjars" % "sockjs-client" % "1.0.2"

libraryDependencies += "org.webjars" % "swagger-ui" % "2.1.1"

libraryDependencies += "org.webjars" % "d3js" % "3.5.5-1"

// For test --------------------------------------------------------------------

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "com.m3" %% "curly-scala" % "0.5.+" % "test"

// An implementation of SLF4J is needed for log in tests to be output
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3" % "test"

// For "sbt console"
unmanagedClasspath in Compile <+= (baseDirectory) map { bd => Attributed.blank(bd / "src/test/resources") }

// For "sbt run/test"
unmanagedClasspath in Runtime <+= (baseDirectory) map { bd => Attributed.blank(bd / "src/test/resources") }

// Generate src/main/scala/xitrum/Version.scala from "version" above -----------

val generateVersionFileTask = TaskKey[Unit]("generateVersion", "Generate src/main/scala/xitrum/Version.scala")

generateVersionFileTask <<= generateVersionFile

(compile in Compile) <<= (compile in Compile) dependsOn (generateVersionFile)

def generateVersionFile = Def.task {
  val versions = version.value.split('.')
  val major    = versions(0).toInt
  val minor    = versions(1).toInt
  val patch    = versions(2).split('-')(0).toInt
  val ma_mi_pa = s"$major.$minor.$patch"
  val base     = (baseDirectory in Compile).value

  // Also check if the directory name is correct
  val resDir = base / s"src/main/resources/META-INF/resources/webjars/xitrum/$ma_mi_pa"
  if (!resDir.exists) throw new Exception(s"Directory name incorrect: $resDir")

  // Do not overwrite version file if its content doesn't change
  val file    = base / "src/main/scala/xitrum/Version.scala"
  val content = s"""// Autogenerated by build.sbt. Do not modify this file directly.
package xitrum
class Version {
  val major = $major
  val minor = $minor
  val patch = $patch
  /** major.minor.patch: $ma_mi_pa */
  override def toString = "$ma_mi_pa"
}
"""
  if (!file.exists) {
    IO.write(file, content)
  } else {
    val oldContent = IO.read(file)
    if (content != oldContent) IO.write(file, content)
  }
}

//------------------------------------------------------------------------------

// Avoid messy Scaladoc by excluding things that are not intended to be used
// directly by normal Xitrum users.
scalacOptions in (Compile, doc) ++= Seq("-skip-packages", "xitrum.sockjs")

// Skip API doc generation to speedup "publish-local" while developing.
// Comment out this line when publishing to Sonatype.
publishArtifact in (Compile, packageDoc) := false
