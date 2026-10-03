#!/bin/zsh
# Build the signed, minified release APK for Firebase App Distribution.
#   tools/release.sh           → build/dist/DubaiNowNext-<version>-<code>.apk
set -e
ROOT="${0:A:h:h}"
export JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@21}"
cd "$ROOT"
./gradlew :app:assembleRelease --console=plain -q 2>&1 | grep -E "^e: |error:|FAIL" || true
VN=$(grep -m1 'versionName = ' app/build.gradle.kts | grep -oE '"[^"]+"' | tr -d '"')
VC=$(grep -m1 'versionCode = ' app/build.gradle.kts | grep -oE '[0-9]+')
mkdir -p build/dist
cp app/build/outputs/apk/release/app-release.apk "build/dist/DubaiNowNext-$VN-$VC.apk"
echo "build/dist/DubaiNowNext-$VN-$VC.apk"
