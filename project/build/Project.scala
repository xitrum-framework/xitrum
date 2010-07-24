import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  val localMavenRepo = "Local Maven Repo" at
    "file://" + Path.userHome + "/.m2/repository"

  val jbossRepo = "JBoss Repo" at
    "https://repository.jboss.org/nexus/content/groups/public/"

  override def libraryDependencies =
    Set(
      "org.jboss.netty"        % "netty"         % "3.2.1.Final"  % "compile->default",
      "org.slf4j"              % "slf4j-log4j12" % "1.6.1"        % "compile->default"
    ) ++ super.libraryDependencies

  override def mainClass = Some("colinh.Http")
}
