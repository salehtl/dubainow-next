#!/bin/zsh
# Build the debug APK, install it on the running emulator and launch with extras.
#   tools/run.sh [key value …]     e.g. tools/run.sh persona sara tab services route topic:health
#   NOBUILD=1 tools/run.sh …       skip the build
#   SERIAL=emulator-5556 tools/run.sh …   pick a device
set -e
ROOT="${0:A:h:h}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ADB="$ANDROID_HOME/platform-tools/adb"
[[ -n "$SERIAL" ]] && ADB="$ADB -s $SERIAL"
cd "$ROOT"
if [[ -z "$NOBUILD" ]]; then
  ./gradlew :app:assembleDebug --console=plain -q 2>&1 | grep -E "^e: |error:" || true
fi
eval $ADB install -r -d app/build/outputs/apk/debug/app-debug.apk >/dev/null
args=()
while (( $# >= 2 )); do args+=(-e "$1" "$2"); shift 2; done
eval $ADB shell am force-stop com.salehtl.dubainownext
eval $ADB shell am start -n com.salehtl.dubainownext/.MainActivity "${(q)args[@]}" >/dev/null
echo "launched ${args[*]}"
