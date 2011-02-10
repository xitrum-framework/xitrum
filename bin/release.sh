#!/bin/sh

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd $ROOT_DIR

# Extract information from project/build.properties
PROJECT_NAME=`   grep project.name         project/build.properties | sed 's/project.name=//g'`
PROJECT_VERSION=`grep project.version      project/build.properties | sed 's/project.version=//g'`
SCALA_VERSION=`  grep build.scala.versions project/build.properties | sed 's/build.scala.versions=//g'`

REL_DIR=target/$PROJECT_NAME-$PROJECT_VERSION
LIB_DIR=$REL_DIR/lib

rm -rf target/$PROJECT_NAME*
mkdir -p $REL_DIR

# lib directory --------------------------------------------------------------

mkdir $LIB_DIR
sbt clean
sbt package
cp target/scala_$SCALA_VERSION/"$PROJECT_NAME"_$SCALA_VERSION-$PROJECT_VERSION.jar $LIB_DIR
cp lib_managed/scala_$SCALA_VERSION/compile/*.jar $LIB_DIR
if [ -d lib ]; then cp lib/*.jar $LIB_DIR; fi
cp project/boot/scala-$SCALA_VERSION/lib/scala-library.jar $LIB_DIR/scala-library-$SCALA_VERSION.jar

# Other default directories --------------------------------------------------

# Do not copy directory (e.g. cp -r) to avoid hidden files
mkdir $REL_DIR/bin
cp bin/runner.sh $REL_DIR/bin

mkdir $REL_DIR/log

mkdir $REL_DIR/config
cp config/* $REL_DIR/config
mv $REL_DIR/config/logback.xml.sample $REL_DIR/config/logback.xml

# TODO: avoid copying hidden files
if [ -d public ]; then cp -r public $REL_DIR; fi

# Your application-specific operations ---------------------------------------

# Compress -------------------------------------------------------------------

cd target
tar cjf $PROJECT_NAME-$PROJECT_VERSION.tar.bz2 $PROJECT_NAME-$PROJECT_VERSION
