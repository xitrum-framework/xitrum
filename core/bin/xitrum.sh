#!/bin/sh

JAVA_OPTS='-Xms2000m -Xmx6000m -server -Djava.awt.headless=true'

# These should be the same as in project/build.properties
APP_VERSION=1.0
APP=myapp

# Not frequently changed
MAIN_CLASS='myapp.Boot'
SCALA_VERSION=2.8.1

CLASS_PATH="lib/*:config"

#-------------------------------------------------------------------------------

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

start() {
  nohup java $JAVA_OPTS -cp $CLASS_PATH $MAIN_CLASS > log/$APP.out 2>&1 &
  ps aux | grep $MAIN_CLASS
}

release() {
  REL_DIR=target/$APP-$APP_VERSION
  LIB_DIR=$REL_DIR/lib

  rm -rf target/$APP*

  # SBT default files ----------------------------------------------------------

  mkdir -p $REL_DIR
  mkdir $LIB_DIR
  sbt clean
  sbt package
  cp target/scala_$SCALA_VERSION/"$APP"_$SCALA_VERSION-$APP_VERSION.jar $LIB_DIR
  cp lib_managed/scala_$SCALA_VERSION/compile/*.jar $LIB_DIR
  if [ -f lib ]; then cp lib/*.jar $LIB_DIR; fi
  cp project/boot/scala-$SCALA_VERSION/lib/scala-library.jar $LIB_DIR/scala-library-$SCALA_VERSION.jar

  # Do not copy directory (e.g. cp -r) to avoid hidden files
  mkdir $REL_DIR/bin
  cp bin/* $REL_DIR/bin

  # Other files ----------------------------------------------------------------

  cp README $REL_DIR
  cp API    $REL_DIR

  mkdir $REL_DIR/log
  mkdir $REL_DIR/config
  cp config/* $REL_DIR/config

  # Final touches --------------------------------------------------------------

  cd target
  tar cjf $APP-$APP_VERSION.tar.bz2 $APP-$APP_VERSION
}

case "$1" in
  release)
    release
    ;;
  *)
    start
esac
