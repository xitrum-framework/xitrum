$ set SCRIPT_DIR=%~dp0
$ java -Xms512M -Xmx1536M -Xss1M -XX:+CMSClassUnloadingEnabled -XX:MaxPermSize=384M -jar "%SCRIPT_DIR%sbt-launch-0.12.4.jar" %*
