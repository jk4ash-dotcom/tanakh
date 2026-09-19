# Seed run 2026-09-19 (cloud Linux VM)

Host had no Android emulator and no AVD. `adb` appeared only after `bootstrap-sdk` installed platform-tools; `adb devices` had no `device`.

## Proven

| Step | Result |
| --- | --- |
| `launch --mode jvm` | READY |
| `doctor` | HEALTHY (JVM). Catalog Jewish order, 39 books, source assets, no INTERNET in source + debug merged manifest |
| `drive pack-sanity` | BUILD SUCCESSFUL. PackSanityTest 17, AssetPathResolutionTest 5, DssVariantRepositoryTest 12 — 0 failures |
| `drive apk-debug` | `verifyDebugApkAssets OK` (packaged `glosses.json` + `Gen.json` after aapt2). `aapt dump permissions` has no `INTERNET` |
| `cleanup` | Removed no evidence. This directory still present afterwards |

## Deferred (not run / not claimed)

| Step | Why |
| --- | --- |
| `drive emulator-smoke` | No `ANDROID_SERIAL` / no emulator. Helper exited without UI dumps. Compose path Home → Genesis → Genesis 1 → 1:1 → gloss sheet is **unproven** |
| `drive apk-release` | Not executed on this seed (debug APK asset + INTERNET check was). Run on a host with `ANDROID_HOME` when you need the release zip |
| About / Notes help UI | No device |
| Isaiah 53:11 Q marker | No device |

Do not treat this seed as Compose UI proof.
