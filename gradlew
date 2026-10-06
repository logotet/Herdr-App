#!/bin/sh
APP_HOME=$(dirname "$0")
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec "${JAVA_HOME:-}/bin/java" -Dorg.gradle.appname=gradlew -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
