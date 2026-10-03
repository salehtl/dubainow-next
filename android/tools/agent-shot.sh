#!/bin/zsh
# For parallel builders sharing emulators. Installs your APK, launches it with extras, waits, and
# screenshots — holding a per-emulator lock so two builders never capture each other's app.
#   tools/agent-shot.sh <serial> <apk> <package> <out.jpg> <wait-seconds> [key value …]
#   e.g. tools/agent-shot.sh emulator-5554 app/build/outputs/apk/debug/app-debug.apk com.salehtl.dubainownext.home /tmp/x.jpg 4 persona sara
set -e
SERIAL="$1"; APK="$2"; PKG="$3"; OUT="$4"; WAIT="$5"; shift 5
ADB="/opt/homebrew/share/android-commandlinetools/platform-tools/adb -s $SERIAL"
LOCK="/tmp/dn-emulator-$SERIAL.lock"
for i in {1..240}; do mkdir "$LOCK" 2>/dev/null && break; sleep 1; done
trap 'rmdir "$LOCK" 2>/dev/null' EXIT
eval $ADB install -r -d "${(q)APK}" >/dev/null
args=(); while (( $# >= 2 )); do args+=(-e "$1" "$2"); shift 2; done
eval $ADB shell am force-stop "$PKG"
eval $ADB logcat -b crash -c 2>/dev/null || true
eval $ADB shell am start -n "$PKG/com.salehtl.dubainownext.MainActivity" "${(q)args[@]}" >/dev/null
sleep "$WAIT"
eval $ADB exec-out screencap -p > "${OUT%.*}.png"
sips -s format jpeg -Z 1000 "${OUT%.*}.png" --out "$OUT" >/dev/null && rm -f "${OUT%.*}.png"
CRASH=$(eval $ADB logcat -d -b crash -t 200 2>/dev/null | grep -A3 "Process: $PKG" | head -8 || true)
[[ -n "$CRASH" ]] && { echo "CRASH:"; echo "$CRASH"; eval $ADB logcat -b crash -c; }
echo "$OUT"
