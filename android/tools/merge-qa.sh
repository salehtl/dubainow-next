#!/bin/zsh
# Merge a QA fixer's owned folder from its work copy (scratchpad/work-android/qa-<owner>) into the project.
#   tools/merge-qa.sh <owner>     owner: shared | personas | welcome | home | widgets | services | search | activity | profile | assistant
set -e
ROOT="${0:A:h:h}"
W="/private/tmp/claude-501/-Users-saleh-Developer-Claude-Workspace-DubaiNow-Next/a423a7e6-7f55-4328-8d48-cc1e30571fe1/scratchpad/work-android/qa-$1"
P=app/src/main/java/com/salehtl/dubainownext
[[ -d "$W/$P" ]] || { echo "missing $W"; exit 1; }
show() { diff -rq "$W/$1" "$ROOT/$1" 2>/dev/null | sed "s|$W/||;s|$ROOT/||" || true; }
case "$1" in
  shared)
    for d in $P/app $P/design res; do [[ $d == res ]] && d=app/src/main/res; show $d; rsync -a --delete --exclude '*.md' "$W/$d/" "$ROOT/$d/"; done
    show $P/data | grep -v personas || true
    rsync -a --delete --exclude personas --exclude '*.md' "$W/$P/data/" "$ROOT/$P/data/"
    for f in $P/MainActivity.kt app/src/main/AndroidManifest.xml; do cmp -s "$W/$f" "$ROOT/$f" || { echo "changed $f"; cp "$W/$f" "$ROOT/$f"; }; done ;;
  personas) show $P/data/personas; rsync -a --delete --exclude '*.md' "$W/$P/data/personas/" "$ROOT/$P/data/personas/" ;;
  *) show $P/features/$1; rsync -a --delete --exclude '*.md' "$W/$P/features/$1/" "$ROOT/$P/features/$1/" ;;
esac
[[ -f "$W/docs/notes/$1.md" ]] && cp "$W/docs/notes/$1.md" "$ROOT/docs/notes/$1.md"
echo "merged qa-$1"
