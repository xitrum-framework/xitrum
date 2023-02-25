organization := "tv.cntt"
name         := "xitrum"
version      := "3.31.0-SNAPSHOT"

// Run "sbt mimaReportBinaryIssues" to check for binary compatibility
// https://github.com/typesafehub/migration-manager
// mimaPreviousArtifacts := Set("tv.cntt" %% "xitrum" % "3.30.2")

//------------------------------------------------------------------------------

// Cannot use 2.12.13+ yet:
// https://github.com/scalate/scalate/issues/309
crossScalaVersions := Seq("2.13.4", "2.12.12")
scalaVersion       := "2.13.4"

// Akka 2.4.0+ requires Java 8
javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")

//------------------------------------------------------------------------------

// From Scala 2.13, to compile XML syntax, the scala.xml package must be on the classpath
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.3.0"

// Projects using Xitrum must provide a concrete implementation of SLF4J (Logback etc.)
libraryDependencies += "tv.cntt" %% "slf4s-api" % "1.7.30"

// Netty is the core of Xitrum's HTTP(S) feature
libraryDependencies += "io.netty" % "netty-all" % "4.1.58.Final"

// https://github.com/netty/netty/wiki/Native-transports
// Only works on Linux
libraryDependencies += "io.netty" % "netty-transport-native-epoll" % "4.1.58.Final" classifier "linux-x86_64"

// https://github.com/netty/netty/wiki/Forked-Tomcat-Native
// https://groups.google.com/forum/#!topic/netty/oRATC6Tl0A4
// Include all classifiers for convenience
libraryDependencies += "io.netty" % "netty-tcnative" % "2.0.36.Final" classifier "linux-x86_64"
libraryDependencies += "io.netty" % "netty-tcnative" % "2.0.36.Final" classifier "osx-x86_64"
libraryDependencies += "io.netty" % "netty-tcnative" % "2.0.36.Final" classifier "windows-x86_64"

// Javassist boosts Netty 4 speed
libraryDependencies += "org.javassist" % "javassist" % "3.27.0-GA"

// For clustering SockJS with Akka
// Redirect Akka log to SLF4J
libraryDependencies += "tv.cntt"           %% "glokka"               % "2.6.1"
libraryDependencies += "com.typesafe.akka" %% "akka-cluster-metrics" % "2.6.11"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j"           % "2.6.11"

// For file watch
// (akka-agent is added here, should ensure same Akka version as above)
libraryDependencies += "com.github.pathikrit" %% "better-files" % "3.9.1"
libraryDependencies += "com.github.pathikrit" %% "better-files-akka"  % "3.9.1"
libraryDependencies += "io.methvin" % "directory-watcher" % "0.16.1"
libraryDependencies += "io.methvin" %% "directory-watcher-better-files" % "0.16.1"

// http://download.oracle.com/javaee/5/api/javax/activation/MimetypesFileTypeMap.html
libraryDependencies += "javax.activation" % "activation" % "1.1.1"

// For scanning routes
libraryDependencies += "tv.cntt" %% "sclasner" % "1.8.0"

// For binary (de)serializing
libraryDependencies += "com.twitter" %% "chill" % "0.9.5"

// For JSON (de)serializing
libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.6.10"

// For i18n
libraryDependencies += "tv.cntt" %% "scaposer" % "1.11.1"

// For jsEscape
libraryDependencies += "org.apache.commons" % "commons-text"  % "1.9"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.11"

// For compiling CoffeeScript to JavaScript
libraryDependencies += "tv.cntt" % "rhinocoffeescript" % "1.12.7"

// For metrics
libraryDependencies += "nl.grons"              %% "metrics4-scala" % "4.1.14"
libraryDependencies += "io.dropwizard.metrics" %  "metrics-json"   % "4.1.17"

// Scala annotations (used by routes and Swagger) compiled by a newer version
// can't be read by an older version.
//
// We must release a new version of Xitrum every time a new version of
// Scala is released.
libraryDependencies += "org.scala-lang" % "scalap" % scalaVersion.value

// WebJars ---------------------------------------------------------------------

libraryDependencies += "org.webjars.bower" % "jquery" % "3.4.1"
libraryDependencies += "org.webjars.bower" % "jquery-validation" % "1.19.1"
libraryDependencies += "org.webjars.bower" % "sockjs-client" % "1.3.0"
libraryDependencies += "org.webjars.bower" % "swagger-ui" % "3.4.0"
libraryDependencies += "org.webjars.bower" % "d3" % "3.5.17"

// For test --------------------------------------------------------------------

// For LruCacheTest
Test / fork := true
Test / javaOptions += "-Dxitrum.mode=production"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.2" % "test"
libraryDependencies += "org.scalatest" %% "scalatest-flatspec" % "3.2.2" % "test"

// For integration tests when a Xitrum server is started
libraryDependencies += "org.asynchttpclient" % "async-http-client" % "2.12.2" % "test"

// An implementation of SLF4J is needed for log in tests to be output
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"

// For "sbt console"
Compile / unmanagedClasspath += baseDirectory.value / "src/test/resources"

// For "sbt run/test"
Runtime / unmanagedClasspath += baseDirectory.value / "src/test/resources"

// Generate src/main/scala/xitrum/Version.scala from "version" above -----------

val generateVersionFileTask = TaskKey[Unit]("generateVersion", "Generate src/main/scala/xitrum/Version.scala")
generateVersionFileTask := generateVersionFile.value

(Compile / compile) := ((Compile / compile) dependsOn generateVersionFile).value
def generateVersionFile = Def.task {
  val versions = version.value.split('.')
  val major    = versions(0).toInt
  val minor    = versions(1).toInt
  val patch    = versions(2).split('-')(0).toInt
  val ma_mi_pa = s"$major.$minor.$patch"
  val base     = (Compile / baseDirectory).value

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
Compile / doc / scalacOptions ++= Seq("-skip-packages", "xitrum.sockjs")

// Skip API doc generation to speedup "publishLocal" while developing.
// Comment out this line when publishing to Sonatype.
Compile / packageDoc / publishArtifact := false
