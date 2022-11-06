Use SBT to compile. See project/build.properties.

Update src/main/resources/META-INF/mime.types
---------------------------------------------

This file is for determining content types for static files.

See:

* https://github.com/klacke/yaws/blob/master/priv/mime.types
* http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types
* http://docs.oracle.com/javase/7/docs/api/javax/activation/MimetypesFileTypeMap.html

On Mac, can use ``opendiff`` GUI tool to compare:

::

  opendiff file1 file2

Debug client-server data receiving and sending
----------------------------------------------

"I recommend Tcpdump / Wireshark, or even better Tcpflow":
http://groups.google.com/group/sockjs/msg/12f28eccf1851d54

Generate API doc with graphical type hierarchy
----------------------------------------------

Demo:
http://xitrum-framework.github.io/api/index.html#xitrum.Action

Graphviz is required, see build.sbt.end.

Create dependency graph
-----------------------

Demo:
http://xitrum-framework.github.io/guide/deps.html

Generate target/dependencies-compile.dot:

::

  sbt dependencyDot

Convert dependencies-compile.dot to deps.png:

::

  dot -Tpng dependencies-compile.dot > deps.png

See:
https://github.com/jrudolph/sbt-dependency-graph.

Publish to local
----------------

While developing, you may need do local publish.

To do local publish for the ``scalaVersion`` specified in ``build.sbt``:

::

  sbt publishLocal

To do local publish for all the ``crossScalaVersions``:

::

  sbt "+ publishLocal"

To delete the local publish:

::

  $ find ~/.ivy2 -name *xitrum* -exec rm -rf {} \;

Publish to Sonatype
-------------------

Before releasing a new Xitrum version, run ``sbt xitrumPackage`` on project
xitrum-new and xitrum-demos, then see directory ``target/xitrum/lib`` to see
if there's any wrong with the versions of .jar files.

See:
https://github.com/sbt/sbt.github.com/blob/gen-master/src/jekyll/using_sonatype.md

Create ~/.sbt/1.0/sonatype.sbt file:

::

  credentials += Credentials("Sonatype Nexus Repository Manager",
                             "oss.sonatype.org",
                             "<your username>",
                             "<your password>")

Then:

1. Temporarily remove ``-SNAPSHOT`` from the version in build.sbt.
   Also comment out ``publishArtifact in (Compile, packageDoc) := false`` in build.sbt.
2. Copy
     dev/build.sbt.end   to the end of build.sbt, and
     dev/plugins.sbt.end to the end of project/plugins.sbt
3. Run ``sbt`` then from SBT command prompt run ``+ publishSigned``.
4. Login at https://oss.sonatype.org/ and from "Staging Repositories" select the
   newly published item, click "Close" then "Release".

Update related projects
-----------------------

After each new version release, please update these projects to use that new Xitrum version:

* `xitrum-new <https://github.com/xitrum-framework/xitrum-new>`_
* `xitrum-demos <https://github.com/xitrum-framework/xitrum-demos>`_
* `xitrum-placeholder <https://github.com/xitrum-framework/xitrum-placeholder>`_
* `xitrum-multimodule-demo <https://github.com/xitrum-framework/xitrum-multimodule-demo>`_
* `comy <https://github.com/xitrum-framework/comy>`_

Also update `gh-pages branch of Xitrum <https://github.com/xitrum-framework/xitrum/tree/gh-pages>`_
and `Xitrum guide <https://github.com/xitrum-framework/xitrum-doc>`_.

When a new SBT generation is released, also update
`xitrum-sbt-plugin <https://github.com/xitrum-framework/xitrum-sbt-plugin>`_.
