#!/bin/zsh
# Exclusive use of one emulator for a multi-step session (parallel agents share the emulators).
#   SERIAL=$(tools/emu-lock.sh acquire <your-name>)   waits (up to 20 min) for a free emulator, prints its serial
#   tools/emu-lock.sh touch <serial>                  keep the lock fresh during long sessions
#   tools/emu-lock.sh release <serial>                ALWAYS release when done
# A lock untouched for 12 minutes is treated as abandoned and taken over.
ADB=/opt/homebrew/share/android-commandlinetools/platform-tools/adb
DIR=/private/tmp/claude-501/-Users-saleh-Developer-Claude-Workspace-DubaiNow-Next/a423a7e6-7f55-4328-8d48-cc1e30571fe1/scratchpad/emu-locks
mkdir -p $DIR
case "$1" in
  acquire)
    for i in {1..1200}; do
      for s in $($ADB devices | awk 'NR>1 && $2=="device"{print $1}'); do
        L=$DIR/$s
        if mkdir $L 2>/dev/null; then echo "${2:-agent}" > $L/owner; echo $s; exit 0; fi
        t=$(stat -f %m $L/owner 2>/dev/null || stat -f %m $L 2>/dev/null || echo 0)
        (( $(date +%s) - t > 720 )) && rm -rf $L
      done
      sleep 1
    done
    echo "no emulator free after 20 min" >&2; exit 1 ;;
  touch) touch $DIR/$2/owner ;;
  release) rm -rf $DIR/$2 ;;
  status) for L in $DIR/*(N); do echo "${L:t} $(cat $L/owner 2>/dev/null) $(( $(date +%s) - $(stat -f %m $L/owner) ))s"; done ;;
  *) echo "usage: emu-lock.sh acquire|touch|release|status" >&2; exit 2 ;;
esac
