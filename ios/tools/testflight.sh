#!/bin/zsh
# Bump the build number, archive, and upload to App Store Connect (internal TestFlight).
#   tools/testflight.sh
set -e
ROOT="${0:A:h:h}"
cd "$ROOT"
CUR=$(grep -m1 "CURRENT_PROJECT_VERSION = " DubaiNowDemo.xcodeproj/project.pbxproj | grep -oE '[0-9]+')
NEXT=$((CUR + 1))
sed -i '' "s/CURRENT_PROJECT_VERSION = $CUR;/CURRENT_PROJECT_VERSION = $NEXT;/g" DubaiNowDemo.xcodeproj/project.pbxproj
echo "Build number $CUR → $NEXT"
rm -rf .build/DubaiNowDemo.xcarchive .build/export
xcodebuild archive -project DubaiNowDemo.xcodeproj -scheme DubaiNowDemo -configuration Release \
  -destination 'generic/platform=iOS' -archivePath .build/DubaiNowDemo.xcarchive \
  -derivedDataPath .build/dd-release -allowProvisioningUpdates 2>&1 | grep -E "error:|ARCHIVE (SUCCEEDED|FAILED)"
xcodebuild -exportArchive -archivePath .build/DubaiNowDemo.xcarchive -exportOptionsPlist Config/ExportOptions.plist \
  -exportPath .build/export -allowProvisioningUpdates 2>&1 | grep -E "error|Upload succeeded|EXPORT (SUCCEEDED|FAILED)"
