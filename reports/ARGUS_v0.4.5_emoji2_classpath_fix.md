# Argus v0.4.5-poc — emoji2 classpath fix (launch crash)

**versionName:** `0.4.5-poc` · **versionCode:** 15

## Root cause (shared with AFC v1.0.9)

v0.4.3/v0.4.4 Gradle `configurations.exclude(androidx.emoji2:*)` removed the
emoji2 AAR from the APK. Compose `ui-text` still ships
`DefaultImpl$getFontLoadState$initCallback$1` whose superclass is
`androidx.emoji2.text.EmojiCompat$InitCallback`. First `Text` composition →
`NoClassDefFoundError` → process dies (installs, will not launch).

Removing empty `InitializationProvider` alone does **not** fix this; the missing
class is emoji2, not Startup. Known-good AFC v1.0.8 still had emoji2 + Startup
on classpath with `EmojiCompatInitializer` present (Argus HIGH open for GMS
font path).

## Fix (Argus HIGH stays closed for GMS font fetch)

1. **Stop excluding** `androidx.emoji2:*` — keep AAR so Compose class hierarchy
   resolves (matches known-good 1.0.8 classpath shape for emoji2).
2. **Keep** `androidx.startup.InitializationProvider` (needed for
   `ProcessLifecycleInitializer`).
3. Manifest `tools:node="remove"` on **only**:
   - `androidx.emoji2.text.EmojiCompatInitializer` (no auto-init → no GMS
     downloadable emoji-font fetch)
   - `androidx.profileinstaller.ProfileInstallerInitializer`
4. Keep `ProfileInstallReceiver` `tools:node="remove"` + Gradle exclude of
   `profileinstaller`.
5. Keep `allowBackup=false`, no `INTERNET`.

## Packaged proof

| Check | Expected |
|-------|----------|
| `EmojiCompat$InitCallback` in dex | PRESENT |
| `InitializationProvider` in dex + manifest | PRESENT |
| `EmojiCompatInitializer` meta-data | ABSENT |
| `ProfileInstallReceiver` | ABSENT |
| `android.permission.INTERNET` | ABSENT |
| Remaining Startup meta-data | `ProcessLifecycleInitializer` only |

## Note

Argus must re-CLEAR before promote.
