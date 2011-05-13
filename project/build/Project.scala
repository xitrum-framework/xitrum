import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  // Compile options

  override def compileOptions = super.compileOptions ++
    Seq("-deprecation",
        "-Xmigration",
        "-Xcheckinit",
        "-encoding", "utf8")
        .map(x => CompileOption(x))

  override def javaCompileOptions = JavaCompileOption("-Xlint:unchecked") :: super.javaCompileOptions.toList

  // Repos ---------------------------------------------------------------------

  // For Netty 3.2.4.Final
  //val jboss = "JBoss" at "https://repository.jboss.org/nexus/content/groups/public/"

  // 3.2.4.Final is stable, but 4.0.0.Alpha1-SNAPSHOT provides POST decoder (including file upload)
  // 4.0.0.Alpha1-SNAPSHOT must be installed to local Maven repository manurally:
  //
  // 1. Quick and dirty
  //
  // Add to Netty's pom.xml
/*
  <repositories>
     <repository>
       <id>repository.jboss.org</id>
       <name>JBoss Releases Repository</name>
       <url>http://repository.jboss.org/maven2</url>
     </repository>
   </repositories>

   <pluginRepositories>
     <pluginRepository>
       <id>repository.jboss.org</id>
       <name>JBoss Releases Repository</name>
       <url>http://repository.jboss.org/maven2</url>
     </pluginRepository>
   </pluginRepositories>
*/
  // wget https://repository.jboss.org/nexus/content/repositories/releases/org/jboss/logging/jboss-logging-spi/2.1.2.GA/jboss-logging-spi-2.1.2.GA.jar
  // mvn install:install-file -DgroupId=org.jboss.logging -DartifactId=jboss-logging-spi -Dpackaging=jar -Dversion=2.1.2.GA -Dfile=jboss-logging-spi-2.1.2.GA.jar -DgeneratePom=true
  // MAVEN_OPTS=-Xmx512m mvn -Dmaven.test.skip=true install
  //
  // 2. Long answer: https://issues.jboss.org/browse/NETTY-387
  val localMaven = "Local Maven Repository" at "file://" + Path.userHome + "/.m2/repository"

  // For Annovention
  val sonatypeSnapshot = "Sonatype Snapshot" at "https://oss.sonatype.org/content/repositories/snapshots"

  override def libraryDependencies =
    Set(
      // Log using SLF4J
      // Projects using Xitrum must provide a concrete implentation (Logback etc.).
      "org.slf4j"       % "slf4j-api"       % "1.6.1" % "provided",

      // Web server
      //"org.jboss.netty" % "netty"           % "3.2.4.Final",
      "org.jboss.netty" % "netty"           % "4.0.0.Alpha1-SNAPSHOT",

      // For scanning all Controllers to build routes
      "tv.cntt"         % "annovention"     % "1.0-SNAPSHOT",

      // For page, action, and object caching
      // Infinispan is good but much heavier, and the logging is bad:
      // https://github.com/infinispan/infinispan/blob/master/core/src/main/java/org/infinispan/util/logging/LogFactory.java
      "com.hazelcast"   % "hazelcast"       % "1.9.3"
    ) ++ super.libraryDependencies

  // Paths ---------------------------------------------------------------------

  // For easier development (sbt console etc.)
  override def unmanagedClasspath = super.unmanagedClasspath +++ "config"

  // Publish -------------------------------------------------------------------

  override def managedStyle = ManagedStyle.Maven

  val publishTo = sonatypeSnapshot

  // ~/.ivy2/.credentials should be:
  // realm=Sonatype Nexus Repository Manager
  // host=oss.sonatype.org
  // user=xxx
  // password=xxx
  Credentials(Path.userHome / ".ivy2" / ".credentials", log)
}
