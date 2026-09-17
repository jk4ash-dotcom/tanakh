# Tanakh Learner

Offline-first Android learner for Hebrew Tanakh.

**Package:** `com.tanakhpoc.learner` · **versionName:** `0.2.1-poc` · **scope:** Torah (Genesis–Deuteronomy)

## What ships in this build

| Layer | Source | Status |
|-------|--------|--------|
| Hebrew tokens | OSHB / morphhb WLC **v.2.2** (all Torah books) | **Real** |
| Phonetics | `hebrew-transliteration` + **Sofer SBL-Learner** (from niqqud) | **Generated** |
| English verse row | **JPS 1917** (PD), VerseMap-aware | **Real** (verse-level) |
| Glosses | **TBESH** primary, **HebrewStrong.xml** fallback | **Real** where lemma resolves |
| Divine name | יהוה / **YHWH** only | **Policy enforced** |
| Nav | **Jewish Tanakh order** | Torah books |
| Assets | Per-book `.json.gz` lazy load | ~1.6MB compressed |

See `docs/SOFER_SBL_LEARNER.md`, `docs/DATA_CONTRACT.md`, `docs/PIPELINE.md`.

## Build & run

```bash
export ANDROID_HOME=/workspace/android-sdk
export JAVA_HOME=/workspace/.jdk/jdk-17.0.20.1+1
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Rebuild offline packs:

```bash
cd tools/pipeline && npm install && npm run build:torah
# Prefer shipping gzip only under app/src/main/assets/data/
```

## UI

- Home → Torah books (Jewish order)
- Book → chapters → verse list
- Verse: LTR **paired chips** (Noto Sans Hebrew + phonetic) in **OSHB token order**
- Tap phonetic → **“Possible sense(s)”** / **“Gloss ≠ verse translation”**

## Quality gates

Pack build runs Sofer hard-fail checks 1–8 (coverage, token order, phonetics, YHWH, gloss sanitize, JPS, LTR, ketiv/qere). Auto K/Q report in `reports/torah-ketiv-qere.md`.
