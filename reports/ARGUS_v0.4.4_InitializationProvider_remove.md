# Argus v0.4.4-poc — empty InitializationProvider remove proof

**versionName:** `0.4.4-poc` · **versionCode:** 14

## Finding (non-blocking follow-up from v0.4.3 CLEAR)

After stripping `EmojiCompatInitializer` / `ProfileInstallerInitializer` meta-data, the leftover empty `androidx.startup.InitializationProvider` remained. Argus: class may not be in the APK; can crash at process start.

## Control

Manifest `tools:node="remove"` on the **provider** itself (not only child meta-data):

```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    tools:node="remove" />
```

Kept from v0.4.3:
- Gradle excludes for `androidx.emoji2:*` and `androidx.profileinstaller:profileinstaller`
- `tools:node="remove"` on `ProfileInstallReceiver`
- No `INTERNET`

Also: `BuildConfig.GIT_SHA` (12-char) stamped at configure time; About screen shows it.

Durable note: skill `Android offline security anti-patterns` item 5 already documents empty-provider removal.

## Merged + packaged proof

`InitializationProvider` hit counts (must be 0):

| Artifact | Hits |
|----------|------|
| merged_manifests/debug | 0 |
| merged_manifests/release | 0 |
| packaged_manifests/debug | 0 |
| packaged_manifests/release | 0 |
| debug APK `aapt dump xmltree` AndroidManifest | 0 |
| release APK `aapt dump xmltree` AndroidManifest | 0 |

Also ABSENT: `EmojiCompatInitializer`, `ProfileInstallReceiver`, `android.permission.INTERNET` (only signature-level `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`).

## Signing / distribute

- Prefer **release** APK (`debuggable=false`) for Protect / Argus sideload.
- Still signed with local debug keystore (CN=Android Debug) — Play Protect may still warn until a real keystore.
- Debug APK attached optionally (debuggable=true).

## Assets (no regress)

`verifyDebugApkAssets` / `verifyReleaseApkAssets` OK — glosses=`glosses.json` (aapt2 plain), Gen=`Gen.json`. PackRepository gz→plain fallback unchanged.

## Tests

`testDebugUnitTest`: 21 tests, 0 failures (AssetPathResolutionTest 5 + PackSanityTest 16).
