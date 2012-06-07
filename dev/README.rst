Notes for Xitrum developers.

Publish to Sonatype
-------------------

1. Copy dev/plugins.sbt to project/plugins.sbt.
2. Copy content of dev/build.sbt.end to the end of build.sbt.
3. Run ``sbt publish`` or ``sbt`` then from SBT command prompt run ``+ publish``.
4. Login at https://oss.sonatype.org/ and from "Staging Repositories" select the
   newly published item, then click "Close".

This workflow is for others to easily do ``sbt publish-local`` without PGP key.
Otherwise there will be error:

::

  java.io.FileNotFoundException: ~/.sbt/gpg/secring.asc (No such file or directory)

Delete local snapshots
----------------------

While developing, you may need do local publish.
Run ``sbt publish-local`` or ``sbt`` then from SBT command prompt run ``+ publish-local``.

To delete:

::

  $ find ~/.ivy2 -name *xitrum* -delete

Netty 4 SNAPSHOT
----------------

**This part will be removed when Netty 3.5.Final is officially released.**

File upload feature in Xitrum needs `Netty <https://github.com/netty/netty>`_ 3.5.Final,
which has not been released. Netty 4 SNAPSHOT is built and put at
https://github.com/ngocdaothanh/xitrum/downloads
