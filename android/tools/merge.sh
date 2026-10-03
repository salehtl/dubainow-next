#!/bin/zsh
# Merge a builder's folder from its working copy into the real project (notes go to docs/notes).
#   tools/merge.sh <item>      e.g. tools/merge.sh home   |   tools/merge.sh personas
set -e
ROOT="${0:A:h:h}"
WORK="/private/tmp/claude-501/-Users-saleh-Developer-Claude-Workspace-DubaiNow-Next/a423a7e6-7f55-4328-8d48-cc1e30571fe1/scratchpad/work-android/$1"
if [[ "$1" == "personas" ]]; then SUB="data/personas"; else SUB="features/$1"; fi
SRC="$WORK/app/src/main/java/com/salehtl/dubainownext/$SUB"
DST="$ROOT/app/src/main/java/com/salehtl/dubainownext/$SUB"
[[ -d "$SRC" ]] || { echo "missing $SRC"; exit 1; }
mkdir -p "$ROOT/docs/notes"
[[ -f "$SRC/NOTES.md" ]] && cp "$SRC/NOTES.md" "$ROOT/docs/notes/$1.md"
rsync -a --delete --exclude '*.md' "$SRC/" "$DST/"
echo "merged $SUB: $(ls "$DST" | wc -l | tr -d ' ') files"
