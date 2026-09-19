---
name: verify-tanakh
description: "Verify Tanakh Learner (com.tanakhpoc.learner), an offline-first Jetpack Compose Android app. Use to launch, doctor, and drive pack/gloss/LTR invariants, APK asset checks, and emulator Compose smoke (verse → chips → gloss sheet)."
---

# Verify Tanakh Learner

Project-local verification skill for the **Tanakh Learner** Android app (`com.tanakhpoc.learner`, `versionName` `0.6.0-template-poc`). Write proofs for the next agent, not a human demo.

**Primary surface:** mobile Android UI (Jetpack Compose, no deep links, no instrumentation tests).  
**Secondary surfaces that already exist and must be reused:** JUnit4 unit tests under `app/src/test/java/com/tanakhpoc/learner/` and Gradle APK asset tasks `verifyDebugApkAssets` / `verifyReleaseApkAssets` in `app/build.gradle.kts`. There is no Playwright, no `androidTest/`, no HTTP API.

**Two Drive planes — do not conflate them:**

| Plane | Needs emulator/device? | What it proves |
| --- | --- | --- |
| **JVM** (`pack-sanity`, `apk-debug`, `apk-release`) | No | Pack/gloss/LTR/YHWH/DSS fixtures, source assets, packaged APK entries, no `INTERNET` in merged manifest |
| **Compose UI** (`emulator-smoke`) | Yes | Real user path: Home → book → chapter → verse → chip → gloss sheet |

A host without `adb` + a device in `device` state **cannot** produce Compose UI proof. JVM success is not Home/Verse UI proof. Do not claim emulator evidence you did not capture.

## Interview (this repo)

- **Surface:** Compose screens `HomeScreen` → `BookScreen` → `ChapterScreen` → `VerseScreen`, plus `AboutScreen`. Catalog loads in `TanakhApp.onCreate`; per-book gzip packs and glosses lazy-load in `PackRepository`.
- **Run:** README: `export ANDROID_HOME=/workspace/android-sdk`, `export JAVA_HOME=/workspace/.jdk/jdk-17.0.20.1+1`, `./gradlew :app:assembleDebug`. Those paths are **optional** on a cloud VM; doctor records whatever JDK/SDK is actually present. `local.properties` is gitignored; helpers write `sdk.dir` when `ANDROID_HOME` is set.
- **Drive:** Existing `PackSanityTest`, `AssetPathResolutionTest`, `DssVariantRepositoryTest`, and `verify*ApkAssets`. Compose Drive uses `adb` + `uiautomator dump` and the accessible names / visible strings below. No `testTag`s exist.
- **Observe:** gradle logs, JUnit XML under `app/build/test-results/testDebugUnitTest`, `unzip -Z1` APK lists, merged `AndroidManifest.xml`, `aapt dump permissions`, uiautomator XML, logcat. App is offline — no network side effects to assert.
- **Isolate:** Package id is a singleton on a device. `launch --mode emulator` refuses to drive an already-installed `com.tanakhpoc.learner` unless `VERIFY_TANAKH_ALLOW_SHARED=1`. JVM gradle runs are isolated by `VERIFY_TANAKH_RUN_ID` evidence dirs; do not start a second emulator Drive against the same serial.

## Launch

There is no long-lived local server.

**JVM (default — use this on a cloud Linux VM without an emulator):**

```bash
export VERIFY_TANAKH_RUN_ID="${VERIFY_TANAKH_RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
.cursor/skills/verify-tanakh/helpers/verify-tanakh launch --mode jvm
```

Ready: helper prints `READY (jvm)` and `helpers/doctor.sh` exits 0. Teardown: `helpers/verify-tanakh cleanup` (nothing to kill).

If `ANDROID_HOME` is missing and you need gradle Drive (unit tests / APK):

```bash
.cursor/skills/verify-tanakh/helpers/verify-tanakh bootstrap-sdk
export ANDROID_HOME=/workspace/android-sdk
```

**Emulator/device (required for Compose flows):**

```bash
export ANDROID_HOME="${ANDROID_HOME:-/workspace/android-sdk}"
export ANDROID_SERIAL="<one serial from adb devices>"
.cursor/skills/verify-tanakh/helpers/verify-tanakh launch --mode emulator
```

Ready: uiautomator dump contains visible text `Tanakh Learner` (Home `TopAppBar` title) and does **not** contain `Could not load catalog`. Catalog load UI string is `Loading Tanakh catalog…` (`NavGraph.kt`). Teardown: `cleanup` uninstalls only if this run installed the APK (state file `artifacts/verify-tanakh/.state/installed-by-us`).

Do not start a second Drive on a serial this run does not own.

## Doctor

Read-only. Run first whenever anything looks off.

```bash
.cursor/skills/verify-tanakh/helpers/verify-tanakh doctor
# Compose-only hosts:
.cursor/skills/verify-tanakh/helpers/verify-tanakh doctor --require-emulator
```

Doctor must report:

- `gradlew` present; `rootProject.name=TanakhLearner`; `applicationId`/`namespace` `com.tanakhpoc.learner`
- `versionName` from `app/build.gradle.kts` (expect `0.6.0-template-poc` at skill authoring)
- Java + optional `ANDROID_HOME`
- Source assets: `app/src/main/assets/data/{catalog.json,glosses.json.gz,books/Gen.json.gz,dss_variants.json}`
- Fixture: `app/src/test/resources/data/pack_torah_samples.json`
- Unit tests and `verifyDebugApkAssets` / `verifyReleaseApkAssets` still registered
- Source `AndroidManifest.xml` has **no** `android.permission.INTERNET` (comment in manifest: offline POC)
- Catalog: `navOrder=Jewish Tanakh`, first book `Gen`, last book `2Chr`, `Dan` after `Esth` (Writings, not after `Ezek`), `totals.books=39`
- Merged manifest INTERNET check **only if** `app/build/intermediates/merged_manifest*` already exists
- `adb` / package install: warn unless `--require-emulator`

Exit `0` = JVM path is worth driving. Missing emulator is a **WARN**, not a FAIL, unless `--require-emulator`.

## Drive

Read the feature map before choosing a path. Prefer existing gradle tests over new scripts.

```bash
.cursor/skills/verify-tanakh/helpers/verify-tanakh drive pack-sanity
.cursor/skills/verify-tanakh/helpers/verify-tanakh drive apk-debug
.cursor/skills/verify-tanakh/helpers/verify-tanakh drive apk-release
.cursor/skills/verify-tanakh/helpers/verify-tanakh drive emulator-smoke
.cursor/skills/verify-tanakh/helpers/verify-tanakh drive all-jvm
```

### JVM — pack / gloss invariants (no emulator)

These are the repo's own tests. Do not duplicate their assertions in ad-hoc Python.

| Command | What it already covers |
| --- | --- |
| `./gradlew :app:testDebugUnitTest --tests com.tanakhpoc.learner.PackSanityTest` | Gen.1.1 token order + phonetics (`בְּרֵאשִׁית` / `bĕrēʾshîth` … `הָאָרֶץ` / `hāʾārets`); Torah sample verses; YHWH `יהוה`/`YHWH` and proclitic `ליהוה`/`laYHWH`; gloss ids `H7225`/`H3068`; `GlossDisplay.displayTokens` never reverses; Jehovah/ye.ho.vah sanitize; ketiv/qere on `Gen.8.17`; JPS verse-level; Deut.6.4 x-large seg flatten |
| `./gradlew :app:testDebugUnitTest --tests com.tanakhpoc.learner.AssetPathResolutionTest` | gz-then-plain candidates; gzip magic; **source** `catalog.json` + `glosses.json.gz` + `books/Gen.json.gz` |
| `./gradlew :app:testDebugUnitTest --tests com.tanakhpoc.learner.DssVariantRepositoryTest` | DSS placement, `ship=false` hides chrome, batch1 `dss_variants.json` (`sofer-signed-batch1`, 16 shipped) |

`drive pack-sanity` runs all three. Fixture `pack_torah_samples.json` is **not** the full 39-book APK packs — full packs are proven by `AssetPathResolutionTest.sourceAssets_*` and `verify*ApkAssets`.

### JVM — release/debug APK assets + offline permission

Already implemented in `app/build.gradle.kts` (`verifyApkAssets`). `:app:check` depends on `verifyDebugApkAssets`.

```bash
./gradlew :app:verifyDebugApkAssets
./gradlew :app:verifyReleaseApkAssets
```

These require `assembleDebug` / `assembleRelease` and assert APK zip entries `assets/data/catalog.json`, glosses `.json.gz` or `.json`, and `books/Gen.json.gz` or `.json` (aapt2 may strip `.gz`). `drive apk-*` runs that task, then checks the merged manifest / `aapt dump permissions` for **no** `android.permission.INTERNET`.

Release is signed with the **debug keystore** (`signingConfig = signingConfigs.getByName("debug")`) — documented in README; not a Play upload key.

### Compose UI — emulator/device only

Routes (`navigation/Routes.kt`): `home`, `about`, `book/{book}`, `chapter/{book}/{chapter}`, `verse/{verseId}`. No intent-filter deep links — navigation is UI only. Activity: `com.tanakhpoc.learner/.MainActivity`.

**Stable handles (from source, not invented):**

| Screen | Handle |
| --- | --- |
| Home title | text `Tanakh Learner` |
| Home About | contentDescription `About` |
| Home notes help | contentDescription `Notes help` |
| Home book card | text `Genesis` (catalog title; first Torah card) |
| Book chapter card | text `Genesis 1` (`BookTitles.chapterLabel`) |
| Chapter verse card | text `1:1` |
| Verse title | text `Genesis 1:1` |
| Verse chip legend | text `Hebrew + phonetic (OSHB order, LTR paired chips)` |
| Verse JPS label | text `JPS 1917 (verse)` |
| Gloss sheet | text `Possible sense(s)` and `Gloss ≠ verse translation` |
| About title | text `About` |
| Back | contentDescription `Back` |
| Qumran marker | contentDescription `Q — Qumran reading` or `Q — {uxLabel}` (e.g. shipped note on `Isa.53.11`) |
| Load | text `Loading Tanakh catalog…` / `Loading Genesis…` |
| Fail | text `Could not load catalog` |

Launcher label (`strings.xml`) is `Tanakh Learner (POC)` — different from the Home title.

`drive emulator-smoke` uses `helpers/uiautomator_drive.py` recipe `verse-gloss`: Home → `Genesis` → `Genesis 1` → `1:1` → tap `bĕrēʾshîth` (or `בְּרֵאשִׁית`) → assert gloss sheet strings and no `jehovah` / `ye.ho.vah`. Chip row is forced LTR in `VerseScreen` (`LocalLayoutDirection.Ltr`).

About: tap Home `About`, assert `Licenses` and `OSHB (PD + CC BY 4.0)` on `AboutScreen`. Notes help: tap `Notes help`, assert dialog title `Notes help` and confirm `Got it`.

Qumran chrome is **not** on Gen.1.1 unless a shipped note targets it; use `Isaiah` → chapter `53` → `53:11` and contentDescription `Q — Qumran reading` (`DssVariantSheet.kt`, batch1 `dss-isa-53-11-a`).

## Evidence

Default directory (survives cleanup):

```
artifacts/verify-tanakh/<VERIFY_TANAKH_RUN_ID>/
```

Override with `VERIFY_TANAKH_EVIDENCE_DIR` (parent) and `VERIFY_TANAKH_RUN_ID`.

**Proof standards:**

- Exercise the real user path or the **existing** unit/APK checks that encode that path. Do not call `PackRepository.clearInstanceForTests` or `parseCombined` from a one-off script and call it UI proof.
- Capture the action and the resulting state: gradle log + JUnit XML; for UI, uiautomator dump **before** the tap and **after** (Home dump, verse dump, gloss dump).
- Side effects: APK zip list; merged manifest INTERNET absence; source assets still on disk. The app writes no user files and has no network — if a Drive talks to the network, you left the app (gradle/SDK download only).
- Mocks: unit tests use `pack_torah_samples.json` and inline DSS JSON. Treat those as fixture proofs. Full-pack proof is `AssetPathResolutionTest.sourceAssets_*` + `verify*ApkAssets`.
- Never report `emulator-smoke` as passed because `pack-sanity` passed.

Minimum JVM proof files: `doctor.txt`, `drive-pack-sanity.log`, `test-results/*.xml`.  
Minimum APK proof: `drive-apk-release.log` or `drive-apk-debug.log`, `apk-*-entries.txt`, `internet-check.txt`.  
Minimum UI proof: `emulator-smoke/01-home.xml` … `05-gloss.xml` plus a screenshot if `adb exec-out screencap -p` works.

## Cleanup

```bash
.cursor/skills/verify-tanakh/helpers/verify-tanakh cleanup
```

- Uninstalls `com.tanakhpoc.learner` **only** if this run wrote `artifacts/verify-tanakh/.state/installed-by-us`.
- Kills an emulator **only** if this run wrote `.state/emulator-pid` (stock launch does not start an AVD; it uses an already-running device).
- Does **not** delete `artifacts/verify-tanakh/<run-id>/`.
- Does **not** `killall java` / `pkill gradle`. `./gradlew --stop` only if `VERIFY_TANAKH_STOP_GRADLE=1`.
- Leaves `local.properties` (gitignored SDK pointer).

After cleanup, confirm the evidence directory still lists `doctor.txt` / drive logs.

## Helpers

All scripts are executable. Dispatcher:

```bash
.cursor/skills/verify-tanakh/helpers/verify-tanakh doctor
.cursor/skills/verify-tanakh/helpers/verify-tanakh launch --mode jvm
.cursor/skills/verify-tanakh/helpers/verify-tanakh drive pack-sanity
.cursor/skills/verify-tanakh/helpers/verify-tanakh cleanup
.cursor/skills/verify-tanakh/helpers/verify-tanakh bootstrap-sdk
```

| Script | Role |
| --- | --- |
| `helpers/verify-tanakh` | Dispatcher |
| `helpers/lib.sh` | Root discovery, `JAVA_HOME`/`ANDROID_HOME`, evidence dir |
| `helpers/doctor.sh` | Read-only health |
| `helpers/launch.sh` | `--mode jvm\|emulator` |
| `helpers/drive.sh` | `pack-sanity\|apk-debug\|apk-release\|emulator-smoke\|all-jvm` |
| `helpers/cleanup.sh` | Teardown; keeps evidence |
| `helpers/bootstrap-android-sdk.sh` | Optional commandlinetools + platform 35 |
| `helpers/uiautomator_drive.py` | Compose recipe `verse-gloss` |

Feature recipes: `.cursor/skills/verify-tanakh/features/`. Keep the map honest with `/maintain-verification-skill` after UI or pack-contract changes.
