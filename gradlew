#!/bin/sh

#
# AstraVeil Gradle wrapper startup script.
#
# NOTE: the binary `gradle/wrapper/gradle-wrapper.jar` is NOT committed to this
# repository (it is a binary artefact). To generate the full wrapper for the
# first time, run `gradle wrapper --gradle-version 8.10.2` from a machine that
# has Gradle installed, then commit the resulting jar. The properties file in
# gradle/wrapper/gradle-wrapper.properties already pins the distribution URL.
#

DIR="$(cd "$(dirname "$0")" && pwd)"
APP_HOME="$DIR"

# Add the wrapper jar to the classpath if it exists.
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  echo "gradle-wrapper.jar not found. Run 'gradle wrapper --gradle-version 8.10.2' once to generate it." >&2
  exit 1
fi

exec java $JAVA_OPTS -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
