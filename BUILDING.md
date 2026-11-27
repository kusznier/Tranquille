# Building

## Requirements

* JDK 21 (the Robolectric UI tests need Java 21; the app itself targets Java 17 bytecode).
* Android SDK with `platforms;android-37.1` and `build-tools;36.0.0`
  (`sdkmanager "platforms;android-37.1" "build-tools;36.0.0"`), and `sdk.dir` in `local.properties`
  or `ANDROID_HOME` pointing to it.
* Network access to Google Maven and Maven Central. Nothing is fetched from JitPack:
  LibPhoneNumberInfo is built from source in [`libs/`](libs/README.md).

## Build and test

```
git clone https://github.com/kusznier/Tranquille.git
cd Tranquille
./gradlew assembleDebug testDebugUnitTest
```

The debug APK is in `app/build/outputs/apk/debug/`.

### Optional: bundle the database

To skip the initial database download after installation, put the `assets/sia` directory from
https://gitlab.com/xynngh/YetAnotherCallBlocker_data into `app/src/main/assets/sia`
(it is ignored by git).

## Signed release

`assembleRelease` signs the APK only when these environment variables are set:
`TRANQUILLE_KEYSTORE_FILE`, `TRANQUILLE_KEYSTORE_PASSWORD`, `TRANQUILLE_KEY_ALIAS`,
`TRANQUILLE_KEY_PASSWORD`. Without them the release APK is unsigned.

In this repository, releases are built by GitHub Actions (`.github/workflows/release.yml`)
with the keystore from repository secrets: bump `versionCode` and `versionName` in
`app/build.gradle.kts`, then run the "Release" workflow from the Actions tab (optionally as a
pre-release). It creates the tag `v<versionName>` and publishes the signed APK with its SHA-256.
