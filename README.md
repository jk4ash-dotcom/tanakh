# Tanakh Learner

Offline-first Android learner for Hebrew Tanakh.

**Package:** `com.tanakhpoc.learner` · **versionName:** `0.4.1-poc` · **scope:** Full Tanakh (Torah + Nevi’im + Ketuvim)

## What ships in this build

| Layer | Source | Status |
|-------|--------|--------|
| Hebrew tokens | OSHB / morphhb WLC **v.2.2** (Full Tanakh (Torah + Nevi’im + Ketuvim)) | **Real** |
| Phonetics | `hebrew-transliteration` + **Sofer SBL-Learner** (from niqqud) | **Generated** |
| English verse row | **JPS 1917** (PD), VerseMap-aware | **Real** (verse-level) |
| Glosses | **TBESH** primary, **HebrewStrong.xml** fallback | **Real** where lemma resolves |
| Divine name | יהוה / **YHWH** only | **Policy enforced** |
| Nav | **Jewish Tanakh order** | Full Tanakh (Torah + Nevi’im + Ketuvim) |
| Assets | Per-book `.json.gz` lazy load | gzip packs |

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
cd tools/pipeline && npm install && npm run build:all
# Prefer shipping gzip only under app/src/main/assets/data/
```

## UI

- Home → Full Tanakh (Torah + Nevi’im + Ketuvim) (Jewish order, division headers)
- Book → chapters → verse list
- Verse: LTR **paired chips** (Noto Sans Hebrew + phonetic) in **OSHB token order**
- Tap phonetic → **“Possible sense(s)”** / **“Gloss ≠ verse translation”**

## Quality gates

Pack build runs Sofer hard-fail checks 1–8 (coverage, token order + x-large/nested-seg flatten, phonetics, YHWH, gloss sanitize, JPS, LTR, ketiv/qere). Auto K/Q reports in `reports/{torah,neviim,ketuvim,tanakh}-ketiv-qere.md`. Sofer ping: `reports/SOFER_PING_TANAKH_v0.4.md`.
