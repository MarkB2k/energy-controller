#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"

./gradlew :app:clean :app:installDist

./gradlew -q :app:runSolarServer &
SOLAR_PID=$!
sleep 2

./gradlew -q :app:runBatteryServer &
BATTERY_PID=$!
sleep 2

./gradlew -q :app:runGridServer &
GRID_PID=$!
sleep 2

trap 'kill $SOLAR_PID $BATTERY_PID $GRID_PID 2>/dev/null || true' EXIT INT TERM

./app/build/install/app/bin/app
