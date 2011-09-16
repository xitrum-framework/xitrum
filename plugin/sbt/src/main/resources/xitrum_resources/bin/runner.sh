#!/bin/sh

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

JAVA_OPTS='-Xms512m -Xmx1024m -XX:MaxPermSize=128m -server -Djava.awt.headless=true -Dxitrum.mode=production'

# slf4j-api is put to the front to avoid error:
# The requested version 1.6 by your slf4j binding is not compatible with [1.5.5, 1.5.6, 1.5.7, 1.5.8]
#
# Include ROOT_DIR to do "ps aux | grep java" to get pid easier when
# starting multiple processes from different directories
CLASS_PATH="lib/slf4j-api-1.6.1.jar:$ROOT_DIR/lib/*:config"

java $JAVA_OPTS -cp $CLASS_PATH $@
