#!/bin/zsh
# Launch the installed demo with args, wait, and save a downsized screenshot.
#   tools/shot.sh <name> [wait-seconds] -- [launch args…]
set -e
NAME="$1"; shift
WAIT=4
if [[ "$1" != "--" ]]; then WAIT="$1"; shift; fi
[[ "$1" == "--" ]] && shift
SIM_NAME="${SIM_NAME:-iPhone 18 Pro}"
UDID=$(xcrun simctl list devices available | grep "$SIM_NAME (" | head -1 | grep -oE '[0-9A-F-]{36}')
OUT="${SHOTS:-/private/tmp/claude-501/-Users-saleh-Developer-Claude-Workspace-DubaiNow-Next/a423a7e6-7f55-4328-8d48-cc1e30571fe1/scratchpad/shots}"
mkdir -p "$OUT"
xcrun simctl launch --terminate-running-process "$UDID" com.salehtl.DubaiNowNext "$@" >/dev/null
sleep "$WAIT"
xcrun simctl io "$UDID" screenshot --type=jpeg "$OUT/$NAME-full.jpg" >/dev/null 2>&1
sips -Z 1000 "$OUT/$NAME-full.jpg" --out "$OUT/$NAME.jpg" >/dev/null
echo "$OUT/$NAME.jpg"
