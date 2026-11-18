#!/usr/bin/env bash
# Production build of the Vaadin app, run with `java -jar` and the JavaFX
# bridge's JVM flags (taken from vaadin-app/pom.xml, so they are defined once).
set -euo pipefail
cd "$(dirname "$0")"
./mvnw -B -q install -DskipTests -Pproduction
cd vaadin-app
flags="$(../mvnw -B -q help:evaluate -Dexpression=javafx.bridge.jvmArgs -DforceStdout)"
applibs="$(../mvnw -B -q help:evaluate -Dexpression=applibs.dir -DforceStdout)"
# shellcheck disable=SC2086
exec java $flags -Dapplibs.dir="$applibs" -jar target/warehouse-javafx-vaadin-app-1.0-SNAPSHOT.jar
