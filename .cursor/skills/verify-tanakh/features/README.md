# Tanakh Learner verification map

This directory is the maintained source for verifying user-facing behavior of Tanakh Learner (`com.tanakhpoc.learner`). Read the index before driving, then use the matching feature file.

## Baseline preconditions

- Repo root contains `settings.gradle.kts` with `rootProject.name = "TanakhLearner"`.
- Set `VERIFY_TANAKH_RUN_ID` so evidence lands in `artifacts/verify-tanakh/$VERIFY_TANAKH_RUN_ID/`.
- Run `.cursor/skills/verify-tanakh/helpers/verify-tanakh doctor` and require `RESULT=HEALTHY`.
- **JVM plane:** `ANDROID_HOME` set (or `bootstrap-sdk`) before any `drive pack-sanity` / `drive apk-*`.
- **Compose plane:** a single `adb` device, launched with `launch --mode emulator`, package installed by this run. Never drive a shared install unless `VERIFY_TANAKH_ALLOW_SHARED=1`.
- Do not rebuild packs (`tools/pipeline`) as part of ordinary verification; drive the assets already under `app/src/main/assets/data/`.

## Driving conventions

- Start from cold Home (`MainActivity`) unless a recipe says otherwise. There are no deep-link intent filters.
- Prefer visible `Text` and `contentDescription` from Compose sources over tap coordinates.
- Treat helper flags and gradle `--tests` class names as literal.
- JVM actions go through `verify-tanakh drive pack-sanity` / `apk-debug` / `apk-release` (wrappers around existing gradle tasks).
- Compose actions go through `verify-tanakh drive emulator-smoke` (`uiautomator_drive.py`).
- Keep proof artifacts. Cleanup must not delete `artifacts/verify-tanakh/<run-id>/`.

## Proof and skip reporting

- Capture the user action and the resulting state, not only the last screen or `BUILD SUCCESSFUL`.
- JVM proof includes the gradle log and JUnit XML. APK proof includes `unzip -Z1` entries and an INTERNET check.
- UI proof includes uiautomator dumps for each navigation step.
- Record the feature ID and plane (`jvm` vs `emulator`) on every artifact.
- An unreachable Compose path on a host without adb is **skipped**, not verified via unit tests. Say so.
- `PackSanityTest` uses `pack_torah_samples.json`, not the full shipped Tanakh packs.

## Feature entry contract

Each feature file starts with an H1 and one paragraph of user-visible behavior, then exactly four H2s: `Sub-features`, `How to get to it (user POV)`, `Driving it with verify-tanakh`, `Gotchas`.

## Features

- [Verse browse](./verse-browse.md) covers Home → book → chapter → verse in Jewish Tanakh order.
- [Word chips](./word-chips.md) covers LTR paired Hebrew/phonetic chips in OSHB token order.
- [Gloss sheet](./gloss-sheet.md) covers tapping a chip for Possible sense(s) and divine-name policy.
- [About and licenses](./about-licenses.md) covers the About screen, Notes help, and license lines.
- [Pack load](./pack-load.md) covers catalog/gloss/book gzip load, APK assets, and no INTERNET.
