# Pipeline how-to (Torah → remaining Tanakh)

## Prerequisites

- Node 20+
- Vendor pins (see `vendor/SOURCE_PINS.md`)
- OSHB morphhb **v.2.2** WLC book XML under `vendor/oshb/`
- JPS 1917 engjps readaloud chapter files under `vendor/jps1917/`
- `vendor/oshb/VerseMap.xml` (WLC ↔ KJV/engjps file refs)
- `vendor/TBESH.txt`, `vendor/HebrewStrong.xml`

## Full Tanakh / Ketuvim build (v0.4.0-poc)

```bash
cd tools/pipeline
npm install
node build-pack.mjs --scope all
# or: node build-pack.mjs --scope ketuvim
# prior: node build-pack.mjs --scope torah+neviim
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

Nevi’im-only rebuild: `node build-pack.mjs --scope neviim` (does not keep Torah in catalog — prefer `torah+neviim`).

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

Sections in Daniel and Ezra are **Biblical Aramaic**. Pipeline flags `aramaic: true` on those books (`ARAMAIC_FLAG_BOOKS`) and **per-token** via OSHB morph segments starting with `A`.

- **Do not** silently apply Hebrew-only Sofer SBL-Learner to Aramaic morph tokens.  
- Sofer-approved handling for v0.4.0-poc: set `aramaic=true` and phonetic `[aramaic-pending]` (skip Hebrew SBL). Hebrew morph tokens in the same book still use SBL-Learner.  
- Nav order remains Jewish Tanakh (Daniel in Ketuvim).

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

## Orphan qere / catchWord (Nevi’im Sofer HIGH)

Some OSHB verses include `<rdg type="x-qere">` **without** a preceding `<w type="x-ketiv">`
(“Adaptations to a Qere which L and BHS, by their design, do not indicate”).

`extractWTokens` in `oshb-w.mjs` must:

1. **Insert** orphan qere words into the token stream (Judg.20.13, 2Sam.8.3, 2Sam.16.23, 2Kgs.19.31, 2Kgs.19.37, Jer.31.38, Jer.50.29, Ruth.3.5, Ruth.3.17).
2. **Replace** `catchWord` surfaces with the qere and store ketiv (Jer.48.44 הניס→הנס).

Hard-fail asserts those verse IDs. See `reports/SOFER_PING_TANAKH_v0.4.md`.

## Prov JPS titleHints

`titleHints` for Proverbs must be anchored (`/^The Proverbs\.?$/i`) so verse 1
“THE PROVERBS of Solomon…” is not treated as a title line (would drop Prov.1.33 / Prov.10.32).


## Ketuvim nested `<seg>` hard-fail

Gated like Torah/Nevi’im flatten parity (`tools/pipeline/hard-fail-checks.mjs`):

| Verse | Type | Surface cons |
|-------|------|--------------|
| Job.38.13 | x-suspended | רשעים |
| Job.38.15 | x-suspended | מרשעים |
| Ps.80.14 | x-suspended | מיער |
| Prov.16.28 | x-small | ונרגן |

Standalone re-check (no rebuild): `node tools/pipeline/run-hard-fail.mjs`

## Proclitic compound gloss patch (small)

Without a full `--scope all` rethink, refresh compound proclitic glosses:

```bash
node tools/pipeline/patch-proclitic-gloss.mjs
```

See Data Contract § Proclitic-only compound lemmas.
