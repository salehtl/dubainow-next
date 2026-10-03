#!/bin/zsh
# Merge a helper's feature folder from its working copy into the real project.
#   tools/merge.sh <feature> [Folder]    e.g. tools/merge.sh home Home
set -e
ROOT="${0:A:h:h}"
WORK="/private/tmp/claude-501/-Users-saleh-Developer-Claude-Workspace-DubaiNow-Next/a423a7e6-7f55-4328-8d48-cc1e30571fe1/scratchpad/work/$1"
FOLDER="${2:-${(C)1}}"
[[ -d "$WORK/DubaiNowDemo/Features/$FOLDER" ]] || { echo "no $WORK/DubaiNowDemo/Features/$FOLDER"; exit 1; }
rsync -a --delete --exclude 'NOTES.md' --exclude '*.md' "$WORK/DubaiNowDemo/Features/$FOLDER/" "$ROOT/DubaiNowDemo/Features/$FOLDER/"
find "$ROOT/DubaiNowDemo/Features/$FOLDER" -name "*.md" -delete
mkdir -p "$ROOT/docs/notes"
[[ -f "$WORK/DubaiNowDemo/Features/$FOLDER/NOTES.md" ]] && cp "$WORK/DubaiNowDemo/Features/$FOLDER/NOTES.md" "$ROOT/docs/notes/$FOLDER.md"
echo "merged $FOLDER: $(ls "$ROOT/DubaiNowDemo/Features/$FOLDER" | wc -l | tr -d ' ') files"
