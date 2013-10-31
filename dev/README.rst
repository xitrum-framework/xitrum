Use SBT 0.13.0 to compile. See project/build.properties.

Update Swagger UI
-----------------

See:

* SwaggerIndexAction
* http://stackoverflow.com/questions/979975/how-to-get-the-value-from-url-parameter

To update Swagger UI, copy dist from https://github.com/wordnik/swagger-ui to
src/main/resources/public/xitrum as swagger-ui. Then replace
"http://petstore.swagger.wordnik.com/api/api-docs" with this function call:

::

  var getUrl = function() {
    if (!location.search) return '/xitrum/swagger.json';

    var parts = location.search.substring(1).split('&');
    for (var i = 0; i < parts.length; i++) {
      var part = parts[i];
      if (part.indexOf('url=') == 0) {
        var url = part.substring('url='.length);
        return url;
      }
    }

    return '/xitrum/swagger.json';
  }

Update src/main/resources/META-INF/mime.types
---------------------------------------------

This file is for determining content types for static files.

See:

* https://github.com/klacke/yaws/blob/master/priv/mime.types
* http://svn.apache.org/repos/asf/httpd/httpd/trunk/docs/conf/mime.types
* http://download.oracle.com/javaee/5/api/javax/activation/MimetypesFileTypeMap.html

Debug client-server data receiving and sending
----------------------------------------------

I recommend Tcpdump / Wireshark, or even better Tcpflow:
http://groups.google.com/group/sockjs/msg/12f28eccf1851d54

Generate API doc with graphical type hierarchy
----------------------------------------------

Demo:
http://ngocdaothanh.github.io/xitrum/api/index.html#xitrum.Action

Graphviz is required, see build.sbt.end.

Create dependency graph
-----------------------

Demo:
http://ngocdaothanh.github.io/xitrum/guide/deps.html

See https://github.com/jrudolph/sbt-dependency-graph.

Convert dependencies-compile.dot to deps.png:

::

  dot -Tpng dependencies-compile.dot > deps.png

Publish to local
----------------

While developing, you may need do local publish. Run
``sbt publish-local``.
Alternatively you can run ``sbt`` then from SBT command prompt run
``+ publish-local``.

To delete the local publish:

::

  $ find ~/.ivy2 -name *xitrum* -delete

Publish to Sonatype
-------------------

See:
https://github.com/sbt/sbt.github.com/blob/gen-master/src/jekyll/using_sonatype.md

Create ~/.sbt/0.13/sonatype.sbt (for SBT 0.12: ~/.sbt/sonatype.sbt) file:

::

  credentials += Credentials("Sonatype Nexus Repository Manager",
                             "oss.sonatype.org",
                             "<your username>",
                             "<your password>")

Then:

1. Copy content of
     dev/build.sbt.end   to the end of build.sbt
     dev/plugins.sbt.end to the end of project/plugins.sbt
2. Run ``sbt publish-signed``. Alternatively you can run ``sbt`` then from SBT
   command prompt run ``+ publish-signed``.
3. Login at https://oss.sonatype.org/ and from "Staging Repositories" select the
   newly published item, click "Close" then "Release".

Update related projects
-----------------------

After each new version release, please update these projects to use that new Xitrum version:

* `xitrum-new <https://github.com/ngocdaothanh/xitrum-new>`_
* `xitrum-demos <https://github.com/ngocdaothanh/xitrum-demos>`_
* `xitrum-placeholder <https://github.com/georgeOsdDev/xitrum-placeholder>`_
* `xitrum-multimodule-demo <https://github.com/ngocdaothanh/xitrum-multimodule-demo>`_
* `comy <https://github.com/ngocdaothanh/comy>`_

Also update `gh-pages branch of Xitrum <https://github.com/ngocdaothanh/xitrum/tree/gh-pages>`_
and `Xitrum guide <https://github.com/ngocdaothanh/xitrum-doc>`_.

When a new SBT generation is released, also update
`xitrum-sbt-plugin <https://github.com/ngocdaothanh/xitrum-sbt-plugin>`_.
