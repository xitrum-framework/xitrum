Publish to local
----------------

While developing, you may need do local publish. Run
``sbt publish-local``.
Alternatively you can run ``sbt`` then from SBT command prompt run
``+ publish-local``.

To delete local publish:

::

  $ find ~/.ivy2 -name *xitrum* -delete

Publish to Sonatype
-------------------

See:
https://github.com/sbt/sbt.github.com/blob/gen-master/src/jekyll/using_sonatype.md

1. Copy dev/plugins.sbt to project/plugins.sbt.
2. Copy content of dev/build.sbt.end to the end of build.sbt.
3. Run ``sbt publish``. Alternatively you can run ``sbt`` then from SBT
   command prompt run ``+ publish``.
4. Login at https://oss.sonatype.org/ and from "Staging Repositories" select the
   newly published item, click "Close" then "Release".

This workflow is for others to easily do ``sbt publish-local`` without PGP key.
Otherwise there will be error:

::

  java.io.FileNotFoundException: ~/.sbt/gpg/secring.asc (No such file or directory)

There are 2 plugins in "plugins" directory.
Publish them the same way as with Xitrum above.
