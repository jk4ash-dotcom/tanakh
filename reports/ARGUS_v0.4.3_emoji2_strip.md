# Argus v0.4.3-poc — EmojiCompatInitializer strip proof

**versionName:** `0.4.3-poc` · **versionCode:** 13

## Controls
1. Manifest `tools:node="remove"` on `androidx.emoji2.text.EmojiCompatInitializer` (StartupProvider meta-data)
2. Manifest `tools:node="remove"` on `ProfileInstallerInitializer` + `ProfileInstallReceiver`
3. Gradle `configurations.configureEach { exclude ... }` for `androidx.emoji2:*` and `androidx.profileinstaller:profileinstaller`

Primary control for Argus HIGH is (1)+(3): initializer cannot auto-run / trigger GMS downloadable emoji-font fetch.

## Merged manifest verification
`./gradlew :app:processDebugMainManifest :app:processReleaseMainManifest` (and full clean assemble):

- **EmojiCompatInitializer:** ABSENT (debug + release merged / packaged manifests)
- **ProfileInstallReceiver:** ABSENT (debug + release)
- **`android.permission.INTERNET`:** ABSENT (only signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`)

## Signing
- Debug APK: debuggable, Android debug keystore
- Release APK: **non-debuggable**, signed with local debug keystore (`signingConfig = signingConfigs.getByName("debug")`) for Play Protect sideload testing — not a Play App Signing upload key (see README)

## Assets (no regress from 0.4.2)
`verifyDebugApkAssets` / `verifyReleaseApkAssets` OK — glosses=`glosses.json` (aapt2 plain), Gen=`Gen.json`. PackRepository gz→plain fallback unchanged.
