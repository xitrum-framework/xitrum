organization := "tv.cntt"
name         := "xitrum"
version      := "3.28.5-SNAPSHOT"

// Run "sbt mima-report-binary-issues" to check for binary compatibility
// https://github.com/typesafehub/migration-manager
//com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
//com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact := Some("tv.cntt" % ("xitrum_" + scalaBinaryVersion.value) % "3.27.0")

//------------------------------------------------------------------------------

// Akka 2.4.0+ dropped Scala 2.10.x support
crossScalaVersions := Seq("2.12.3", "2.11.11")
scalaVersion       := "2.12.3"

// Akka 2.4.0+ requires Java 8
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

// For LruCacheTest
fork in Test := true
javaOptions in Test += "-Dxitrum.mode=production"

//------------------------------------------------------------------------------

// Projects using Xitrum must provide a concrete implementation of SLF4J (Logback etc.)
libraryDependencies += "tv.cntt" %% "slf4s-api" % "1.7.25"

// Netty is the core of Xitrum's HTTP(S) feature
libraryDependencies += "io.netty" % "netty-all" % "4.1.15.Final"

// https://github.com/netty/netty/wiki/Native-transports
// Only works on Linux
libraryDependencies += "io.netty" % "netty-transport-native-epoll" % "4.1.15.Final" classifier "linux-x86_64"

// https://github.com/netty/netty/wiki/Forked-Tomcat-Native
// https://groups.google.com/forum/#!topic/netty/oRATC6Tl0A4
// Include all classifiers for convenience
libraryDependencies += "io.netty" % "netty-tcnative" % "2.0.6.Final" classifier "linux-x86_64"
libraryDependencies += "io.netty" % "netty-tcnative" % "2.0.6.Final" classifier "osx-x86_64"
libraryDependencies += "io.netty" % "netty-tcnative" % "2.0.6.Final" classifier "windows-x86_64"

// Javassist boosts Netty 4 speed
libraryDependencies += "org.javassist" % "javassist" % "3.21.0-GA"

// Redirect Akka log to SLF4J
libraryDependencies += "com.typesafe.akka" %% "akka-actor"           % "2.5.4"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster"         % "2.5.4"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-metrics" % "2.5.4"
libraryDependencies += "com.typesafe.akka" %% "akka-contrib"         % "2.5.4"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j"           % "2.5.4"

// For clustering SockJS with Akka
libraryDependencies += "tv.cntt" %% "glokka" % "2.5.0"

// For file watch
// (akka-agent is added here, should ensure same Akka version as above)
libraryDependencies += "com.beachape.filemanagement" %% "schwatcher" % "0.3.3"

// For scanning routes
libraryDependencies += "tv.cntt" %% "sclasner" % "1.7.0"

// For binary (de)serializing
libraryDependencies += "com.twitter" %% "chill" % "0.9.2"

// For JSON (de)serializing
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.5.3"

// For i18n
libraryDependencies += "tv.cntt" %% "scaposer" % "1.10"

// For jsEscape
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.6"

// For compiling CoffeeScript to JavaScript
libraryDependencies += "tv.cntt" % "rhinocoffeescript" % "1.10.0"

// For metrics
libraryDependencies += "nl.grons" %% "metrics-scala" % "3.5.9"
libraryDependencies += "io.dropwizard.metrics" % "metrics-json" % "3.2.4"

// JSON4S uses scalap 2.10.0/2.11.0, which in turn uses scala-compiler 2.10.0/2.11.0, which in
// turn uses scala-reflect 2.10.0/2.11.0. We need to force "scalaVersion" above, because
// Scala annotations (used by routes and Swagger) compiled by a newer version
// can't be read by an older version.
//
// Also, we must release a new version of Xitrum every time a new version of
// Scala is released.
libraryDependencies += "org.scala-lang" % "scalap" % scalaVersion.value

// WebJars ---------------------------------------------------------------------

libraryDependencies += "org.webjars.bower" % "jquery" % "3.2.1"
libraryDependencies += "org.webjars.bower" % "jquery-validation" % "1.16.0"
libraryDependencies += "org.webjars.bower" % "sockjs-client" % "1.1.4"
libraryDependencies += "org.webjars.bower" % "swagger-ui" % "3.0.20"
libraryDependencies += "org.webjars.bower" % "d3" % "3.5.17"

// For test --------------------------------------------------------------------

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.4" % "test"

libraryDependencies += "org.asynchttpclient" % "async-http-client" % "2.0.34" % "test"

// An implementation of SLF4J is needed for log in tests to be output
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"

// For "sbt console"
unmanagedClasspath in Compile += Attributed.blank(baseDirectory.value / "src/test/resources")

// For "sbt run/test"
unmanagedClasspath in Runtime += Attributed.blank(baseDirectory.value / "src/test/resources")

// Generate src/main/scala/xitrum/Version.scala from "version" above -----------

val generateVersionFileTask = TaskKey[Unit]("generateVersion", "Generate src/main/scala/xitrum/Version.scala")
generateVersionFileTask := generateVersionFile

(compile in Compile) <<= (compile in Compile) dependsOn generateVersionFile
def generateVersionFile = Def.task {
  val versions = version.value.split('.')
  val major    = versions(0).toInt
  val minor    = versions(1).toInt
  val patch    = versions(2).split('-')(0).toInt
  val ma_mi_pa = s"$major.$minor.$patch"
  val base     = (baseDirectory in Compile).value

  // Check if the resource directory name contains version
  val resDir = base / s"src/main/resources/META-INF/resources/webjars/xitrum/$ma_mi_pa"
  if (!resDir.exists)
    throw new IllegalStateException(s"Directory name incorrect: $resDir")

  // Check if the URL to xitrum.js contains the version
  val xitrumJsFile = base / "src/main/scala/xitrum/js.scala"
  val expectedUrl  = s"""@GET("xitrum/xitrum-$ma_mi_pa.js")"""
  if (!IO.read(xitrumJsFile).contains(expectedUrl))
    throw new IllegalStateException(s"Incorrect URL to xitrum.js in $xitrumJsFile, should be: $expectedUrl")

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
