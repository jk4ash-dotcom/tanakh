# Pipeline how-to (Torah → remaining Tanakh)

## Prerequisites

- Node 20+
- Vendor pins (see `vendor/SOURCE_PINS.md`)
- OSHB morphhb **v.2.2** WLC book XML under `vendor/oshb/`
- JPS 1917 engjps readaloud chapter files under `vendor/jps1917/`
- `vendor/oshb/VerseMap.xml` (WLC ↔ KJV/engjps file refs)
- `vendor/TBESH.txt`, `vendor/HebrewStrong.xml`

## Torah build (this checkpoint)

```bash
cd tools/pipeline
npm install
node build-pack.mjs --scope torah
# optional: keep pretty JSON for debugging
# node build-pack.mjs --scope torah --pretty --no-gzip
```

Outputs:

| Path | Role |
|------|------|
| `app/src/main/assets/data/catalog.json` | Book index (Jewish order) |
| `app/src/main/assets/data/books/{Gen,Exod,Lev,Num,Deut}.json.gz` | Per-book verse packs |
| `app/src/main/assets/data/glosses.json.gz` | Shared gloss catalog |
| `reports/torah-ketiv-qere.{json,md}` | Auto K/Q list |
| `reports/torah-hard-fail.json` | Sofer checks 1–8 |
| `reports/torah-gaps.json` | Non-fatal gloss/phonetic notes |

After build, **ship gzip assets only** in the APK (delete uncompressed `.json` book/gloss files if present) to keep size ~2MB content.

## Hard-fail checks (Sofer 1–8)

Run automatically at end of `build-pack.mjs`. Failures exit non-zero.

1. **Coverage** — chapters/verses present; Torah chapter counts; Jewish catalog order  
2. **Token order** — OSHB order; Gen.1.1 golden tokens  
3. **Phonetics** — non-blank; YHWH tokens = `YHWH`; no `[transliteration-error]`  
4. **YHWH** — `he` = `יהוה`, `phonetic` = `YHWH`, `divineName`  
5. **GlossDisplay sanitize** — Jehovah / ye.ho.vah / Adonai-vowel dumps stripped in display path  
6. **JPS verse-level** — every verse has English; not word-aligned  
7. **LTR `displayTokens`** — never reverses `words[]`  
8. **Ketiv/Qere** — `qereFlag` + `ketiv`; report complete; phonetic from qere  

## Extending beyond Torah

1. Extract additional `vendor/oshb/*.xml` from morphhb **v.2.2** zip (39 books, no deuterocanon).  
2. Extract matching `engjps_*_{CODE}_*_read.txt` from ebible engjps readaloud zip.  
3. Add book entries to `tools/pipeline/books.mjs` (OSHB file, JPS num/code, division).  
4. Keep **`JEWISH_TANAKH_ORDER`** for navigation — never Christian/filename order (Daniel in Ketuvim).  
5. Build: `node build-pack.mjs --scope book=Isa` (or extend scope when ready).  
6. App already lazy-loads per-book `.json.gz` via catalog.

Full corpus size ballpark: **~10–25MB compressed** content.

## Versification

- **Hebrew WLC verse IDs are primary** (`Gen.32.1`, …).  
- JPS engjps chapter files follow KJV organization; use `VerseMap.xml` (`wlc` → `kjv`) when looking up English.  
- Mismatches are real (e.g. Gen 31/32, Exod 7/8). Do not invent alignments.

## Hebrew normalization

**Do not NFC-normalize** Hebrew surfaces. Strip cantillation for UI; keep niqqud as in OSHB.

Nested OSHB `<seg>` (e.g. `type="x-large"`) inside `<w>` must be **flattened** (concatenate text nodes; ignore seg tags for surface form) — see `tools/pipeline/oshb-w.mjs`. Hard-fail token-count uses the same flatten.

## Biblical Aramaic (Dan / Ezra)

Sections in Daniel and Ezra are **Biblical Aramaic**. Pipeline flags `aramaic: true` on those books (`ARAMAIC_FLAG_BOOKS`).

- **Do not** silently apply Hebrew-only Sofer SBL-Learner as if the text were Hebrew.  
- Require Sofer-approved Aramaic handling before shipping phonetics for those spans.  
- N/A for Torah body; the gate exists so Nevi’im/Ketuvim work does not regress.

## Single book rebuild

```bash
node build-pack.mjs --scope book=Gen
```

## App rebuild

```bash
export ANDROID_HOME=/workspace/android-sdk
export JAVA_HOME=/workspace/.jdk/jdk-17.0.20.1+1
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
