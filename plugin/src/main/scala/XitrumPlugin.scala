import java.io.FileOutputStream

import sbt._
import Keys._

object XitrumPlugin extends Plugin {
  // Must be lazy to avoid null error
  // xitrumPackageNeedsPackageBin must be after xitrumPackageImpl
  override lazy val settings = Seq(newTask, xitrumPackageImpl, xitrumPackageNeedsPackageBin)

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

  // --------------------------------------------------------------------------

  val newKey = TaskKey[Unit]("xitrum-new", "Prepares files for a new SBT project, ready for development")

  lazy val newTask = newKey <<= baseDirectory map { baseDir =>
    (baseDir / "config").mkdir
    copyResourceFile(baseDir, "config/hazelcast_cluster_member_or_super_client.xml")
    copyResourceFile(baseDir, "config/hazelcast_java_client.properties")
    copyResourceFile(baseDir, "config/logback.xml")
    copyResourceFile(baseDir, "config/xitrum.properties")

    (baseDir / "static").mkdir
    copyResourceFile(baseDir, "static/404.html")
    copyResourceFile(baseDir, "static/500.html")
    copyResourceFile(baseDir, "static/favicon.ico")
    copyResourceFile(baseDir, "static/robots.txt")

    (baseDir / "static" / "public" / "css" / "960").mkdirs
    copyResourceFile(baseDir, "static/public/css/app.css")
    copyResourceFile(baseDir, "static/public/css/960/reset.css")
    copyResourceFile(baseDir, "static/public/css/960/text.css")
    copyResourceFile(baseDir, "static/public/css/960/960.css")

    (baseDir / "src" / "main" / "scala" / "my_project" / "action").mkdirs
    copyResourceFile(baseDir, "src/main/scala/my_project/Boot.scala")
    copyResourceFile(baseDir, "src/main/scala/my_project/action/AppAction.scala")
    copyResourceFile(baseDir, "src/main/scala/my_project/action/IndexAction.scala")

    copyResourceFile(baseDir, "build.sbt")
    copyResourceFile(baseDir, "README")
  }

  // --------------------------------------------------------------------------

  val xitrumPackage = TaskKey[Unit]("xitrum-package", "Package to target/xitrum_package directory, ready to deploy to production server")

  // Must be lazy to avoid null error
  lazy val xitrumPackageImpl = xitrumPackage <<=
      (externalDependencyClasspath in Runtime, baseDirectory, target, scalaVersion) map {
      (libs,                                   baseDir,       target, scalaVersion) =>

    val packageDir = target / "xitrum_package"

    // Copy bin directory
    val binDir1 = baseDir    / "bin"
    val binDir2 = packageDir / "bin"
    binDir2.mkdirs
    copyResourceFile(packageDir, "bin/runner.sh")
    IO.copyDirectory(binDir1, binDir2)
    binDir2.listFiles.foreach { _.setExecutable(true) }

    // Copy config directory
    val configDir1 = baseDir    / "config"
    val configDir2 = packageDir / "config"
    IO.copyDirectory(configDir1, configDir2)

    // Copy public directory
    val publicDir1 = baseDir    / "public"
    val publicDir2 = packageDir / "public"
    IO.copyDirectory(publicDir1, publicDir2)

    // Copy lib directory
    val libDir = packageDir / "lib"

    // Copy dependencies
    libs.foreach { lib =>
      val file = lib.data

      // Prevent copying directories in classpath, e.g. "config" directory
      if (!file.isDirectory) IO.copyFile(file, libDir / file.name)
    }

    // Copy .jar files are created after running "sbt package"
    val jarDir = new File(target, "scala-" + scalaVersion.replace('-', '.'))
    (jarDir * "*.jar").get.foreach { file => IO.copyFile(file, libDir / file.name) }
  }

  val xitrumPackageNeedsPackageBin = xitrumPackage <<= xitrumPackage.dependsOn(packageBin in Compile)
}
