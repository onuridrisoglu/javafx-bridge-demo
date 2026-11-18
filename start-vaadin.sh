#!/usr/bin/env bash
# Builds everything and runs the Vaadin app (dev mode) on http://localhost:8889,
# with WarehousePro bridged into it through the JavaFX bridge.
set -euo pipefail
cd "$(dirname "$0")"
# Free the debugger port a previous run may have left behind.
lsof -ti tcp:5006 | xargs kill 2>/dev/null || true
./mvnw -B -q install -DskipTests
cd vaadin-app
../mvnw -B spring-boot:run
