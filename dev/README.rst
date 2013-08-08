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
* `xitrum-modularized-demo <https://github.com/ngocdaothanh/xitrum-modularized-demo>`_
* `comy <https://github.com/ngocdaothanh/comy>`_

Also update `gh-pages branch of Xitrum <https://github.com/ngocdaothanh/xitrum/tree/gh-pages>`_
and `Xitrum guide <https://github.com/ngocdaothanh/xitrum-doc>`_.
