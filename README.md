# Tanakh Learner (POC)

Offline-first Android POC for learning Hebrew Tanakh (Genesis 1–3).

**Package:** `com.tanakhpoc.learner` · **versionName:** `0.1.3-poc`

## What ships in this build

| Layer | Source | Status |
|-------|--------|--------|
| Hebrew tokens | OSHB / morphhb WLC **v.2.2** `Gen.xml` | **Real** |
| Phonetics | `hebrew-transliteration` + **Sofer SBL-Learner** schema (from niqqud) | **Generated (real pipeline)** |
| English verse row | **JPS 1917** (Public Domain) | **Real** (verse-level, not word-aligned) |
| Glosses | **TBESH** primary, **HebrewStrong.xml** fallback | **Real** where lemma resolves |
| Divine name | יהוה / **YHWH** only | **Policy enforced** |

See `docs/SOFER_SBL_LEARNER.md` and `docs/DATA_CONTRACT.md`.

## Build & run

```bash
export ANDROID_HOME=/path/to/Android/sdk
export JAVA_HOME=/path/to/jdk-17
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Rebuild the offline pack after changing vendor data:

```bash
cd tools/pipeline && npm install && node build-pack.mjs
```

## Fonts

Hebrew UI uses embedded **Noto Sans Hebrew** (SIL Open Font License 1.1) from the [Noto Hebrew](https://github.com/notofonts/hebrew) project — see `third_party/NotoSansHebrew/` and `app/src/main/res/font/`. Applied to verse chips and gloss-sheet Hebrew so devices without system Hebrew fonts still render glyphs.

## UI

- Home → Genesis 1 / 2 / 3
- Verse: LTR **paired chips** (RTL Hebrew glyphs inside chip + LTR phonetic) in **OSHB token order**
- Tap phonetic → bottom sheet **“Possible sense(s)”** with footer **“Gloss ≠ verse translation”**
- About: licenses, Sofer policy, LTR display note

## Data contract (Sleuth / Sofer)

Pack asset: `app/src/main/assets/data/pack_gen_1_3.json`

- Single `words[]` array per verse — never reverse Hebrew independently of phonetics
- English is verse-level JPS 1917
- Phonetics derived from OSHB niqqud only (never from English)
