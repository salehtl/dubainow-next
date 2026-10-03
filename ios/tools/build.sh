#!/bin/zsh
# Build the demo for the Simulator and (optionally) install + launch it.
#   tools/build.sh                 build only
#   tools/build.sh run [args…]     build, install on the booted "iPhone 18 Pro" and launch with args
set -e
ROOT="${0:A:h:h}"
DD="${DD:-$ROOT/.build/dd}"
SIM_NAME="${SIM_NAME:-iPhone 18 Pro}"
cd "$ROOT"
xcodebuild build -project DubaiNowDemo.xcodeproj -scheme DubaiNowDemo \
  -destination "platform=iOS Simulator,name=$SIM_NAME" -derivedDataPath "$DD" 2>&1 \
  | grep -E "error:|warning:|BUILD (SUCCEEDED|FAILED)" | grep -v appintents || true
if [[ "$1" == "run" ]]; then
  shift
  UDID=$(xcrun simctl list devices available | grep "$SIM_NAME (" | head -1 | grep -oE '[0-9A-F-]{36}')
  xcrun simctl boot "$UDID" 2>/dev/null || true
  xcrun simctl install "$UDID" "$DD/Build/Products/Debug-iphonesimulator/DubaiNowDemo.app"
  xcrun simctl launch --terminate-running-process "$UDID" com.salehtl.DubaiNowNext "$@"
fi
