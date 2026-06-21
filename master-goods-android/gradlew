#!/bin/sh

APP_BASE_NAME=`basename "$0"`
APP_HOME=`dirname "$0"`

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec java -Xmx2048m -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
