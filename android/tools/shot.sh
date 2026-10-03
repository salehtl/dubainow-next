#!/bin/zsh
# Screenshot the emulator to a downsized JPEG:  tools/shot.sh out.jpg [wait-seconds]
set -e
OUT="$1"; WAIT="${2:-3}"
export ANDROID_HOME="${ANDROID_HOME:-/opt/homebrew/share/android-commandlinetools}"
ADB="$ANDROID_HOME/platform-tools/adb"; [[ -n "$SERIAL" ]] && ADB="$ADB -s $SERIAL"
sleep "$WAIT"
eval $ADB exec-out screencap -p > "${OUT%.*}.png"
sips -s format jpeg -Z 1000 "${OUT%.*}.png" --out "$OUT" >/dev/null && rm "${OUT%.*}.png"
echo "$OUT"
