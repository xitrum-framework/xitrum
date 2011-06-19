import java.io.FileOutputStream

import sbt._
import Keys._

object XitrumPlugin extends Plugin {
  // settings must be lazy to avoid null error
  // distNeedsPackageBin must be after distTask
  override lazy val settings = Seq(newTask, distTask, distNeedsPackageBin)

  //----------------------------------------------------------------------------

  def copyResourceFile(destDir: File, relativePath: String) {
    val fromPath = "xitrum_resources/" + relativePath
    val toPath   = destDir + "/" + relativePath

    val inputStream = getClass.getClassLoader.getResourceAsStream(fromPath)
    val f = new File(toPath)
    val ouputStream = new FileOutputStream(f)
    val buf = new Array[Byte](1024)
    var len = inputStream.read(buf)
    while (len > 0) {
      ouputStream.write(buf, 0, len)
      len = inputStream.read(buf)
    }
    ouputStream.close
    inputStream.close
  }

  // Task "new" ----------------------------------------------------------------

  val newKey = TaskKey[Unit]("xitrum-new", "Prepares files for a new SBT project, ready for development")

  lazy val newTask = newKey <<= baseDirectory map { baseDir =>
    (baseDir / "config").mkdir
    copyResourceFile(baseDir, "config/hazelcast.xml")
    copyResourceFile(baseDir, "config/logback.xml")
    copyResourceFile(baseDir, "config/xitrum.properties")

    (baseDir / "public").mkdir
    copyResourceFile(baseDir, "public/404.html")
    copyResourceFile(baseDir, "public/500.html")
    copyResourceFile(baseDir, "public/favicon.ico")
    copyResourceFile(baseDir, "public/robots.txt")

    (baseDir / "src" / "main" / "scala" / "my_project" / "action").mkdirs
    copyResourceFile(baseDir, "src/main/scala/my_project/Boot.scala")
    copyResourceFile(baseDir, "src/main/scala/my_project/action/AppAction.scala")
    copyResourceFile(baseDir, "src/main/scala/my_project/action/IndexAction.scala")

    copyResourceFile(baseDir, "build.sbt")
  }

  // Task "dist" ---------------------------------------------------------------

  val distKey = TaskKey[Unit]("xitrum-dist", "Prepares target/dist directory, ready for production distribution")

  // distTask must be lazy to avoid null error
  lazy val distTask = distKey <<=
      (externalDependencyClasspath in Runtime, baseDirectory, target, scalaVersion) map {
      (libs,                                   baseDir,       target, scalaVersion) =>

    val distDir = target / "dist"

    // Copy bin directory
    val binDir1 = baseDir / "bin"
    val binDir2 = distDir / "bin"
    binDir2.mkdirs
    copyResourceFile(distDir, "bin/runner.sh")
    IO.copyDirectory(binDir1, binDir2)
    binDir2.listFiles.foreach { _.setExecutable(true) }

    // Copy config directory
    val configDir1 = baseDir / "config"
    val configDir2 = distDir / "config"
    IO.copyDirectory(configDir1, configDir2)

    // Copy public directory
    val publicDir1 = baseDir / "public"
    val publicDir2 = distDir / "public"
    IO.copyDirectory(publicDir1, publicDir2)

    // Copy lib directory
    val libDir = distDir / "lib"

    // Copy dependencies
    libs.foreach { lib => IO.copyFile(lib.data, libDir / lib.data.name) }

    // Copy .jar files are created after running "sbt package"
    val jarDir = new File(target, "scala-" + scalaVersion.replace('-', '.'))
    (jarDir * "*.jar").get.foreach { file => IO.copyFile(file, libDir / file.name) }
  }

  val distNeedsPackageBin = distKey <<= distKey.dependsOn(packageBin in Compile)
}
