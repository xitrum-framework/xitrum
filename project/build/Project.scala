import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  val localMavenRepo = "Local Maven Repo" at
    "file://" + Path.userHome + "/.m2/repository"

  val jbossRepo = "JBoss Repo" at
    "https://repository.jboss.org/nexus/content/groups/public/"

  val scalateRepo = "Scalate Repo" at
    "http://repo.fusesource.com/nexus/content/repositories/snapshots"

  override def libraryDependencies =
    Set(
      "org.jboss.netty"        % "netty"         % "3.2.1.Final"  % "compile->default",
      "org.fusesource.scalate" % "scalate-core"  % "1.2-SNAPSHOT" % "compile->default",
      "org.slf4j"              % "slf4j-log4j12" % "1.6.1"        % "compile->default"
    ) ++ super.libraryDependencies

  override def mainClass = Some("colinh.Http")
}
