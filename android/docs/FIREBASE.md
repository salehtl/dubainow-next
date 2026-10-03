# Sharing the Android demo with Firebase App Distribution

Firebase project: **dubainownext** (project number 682586356868)
Firebase Android app ID: `1:682586356868:android:284a3f974f12be761ed6d1` (registered 3 Oct 2026)
App: **DubaiNow Next**, package `com.salehtl.dubainownext`, version 6.0 (1)
APK: `android/build/dist/DubaiNowNext-6.0-1.apk` (built by `tools/release.sh`, signed with the upload key)

## One-time setup (Firebase console)
1. Open console.firebase.google.com → project **dubainownext**.
2. ~~Project overview → **Add app** → Android (`com.salehtl.dubainownext`)~~ — done 3 Oct.
   The demo uses no Firebase SDK, so `google-services.json` stays out of the project.
3. Left menu → Release & Monitor → **App Distribution** → Get started.
4. Testers & Groups → add a group (e.g. "Leadership") and add your boss's email address.

## Each release
1. `tools/release.sh` → builds `build/dist/DubaiNowNext-<version>-<build>.apk`.
   Bump `versionCode` in `app/build.gradle.kts` first, or Android won't update the installed app.
2. App Distribution → **Releases** → drag the APK in → add the group → add release notes → **Distribute**.

## What your boss does (once)
1. Opens the invitation email on the Android phone → **Get started** → accepts with a Google account.
2. Installs the **Firebase App Tester** app when prompted (or downloads the APK from the email link).
3. Allows "Install unknown apps" for App Tester / Chrome when Android asks.
4. Opens DubaiNow Next → picks a persona on the welcome screen (Sara is the main demo).
   Later releases appear in App Tester with an **Update** button.

## Keep safe
`android/dubainow-upload.jks` and `android/keystore.properties` (the password). Every future release must
be signed with the same key, or testers have to uninstall before installing the new build. Back both up
somewhere private (e.g. your password manager); they're excluded from git.

## Optional: upload from the terminal
```bash
npm install -g firebase-tools
firebase login
firebase appdistribution:distribute build/dist/DubaiNowNext-6.0-1.apk \
  --app 1:682586356868:android:284a3f974f12be761ed6d1 --groups leadership --release-notes "Demo build"
```
