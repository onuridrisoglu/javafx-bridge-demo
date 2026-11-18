#!/usr/bin/env bash
# Builds and runs WarehousePro 3.5 as a JavaFX desktop application.
set -euo pipefail
cd "$(dirname "$0")"
./mvnw -B -q install -pl warehouse-core,javafx-app -am -DskipTests
./mvnw -B -q -pl javafx-app javafx:run
