# Critic HIGH fix — `isAramaicMorph` (v0.4.1-poc)

## Bug
`.split('/').some(/^A/)` treated OSHB **Hebrew adjective** POS segments (`/A…`, e.g. `HTd/Aamsa`) as Biblical Aramaic. Those tokens were baked as `[aramaic-pending]` / `aramaic: true`.

## Correct rule
Language is the **first character of the morph string only**:
- `/^A/.test(morph)` → Aramaic
- `/^H/.test(morph)` → Hebrew  
Do **not** scan `/`-separated POS segments.

## Fix sites
- `tools/pipeline/morph-lang.mjs` — shared `isAramaicMorph`
- `tools/pipeline/build-pack.mjs` — pack phonetics
- `tools/pipeline/hard-fail-checks.mjs` — phonetics gate + Gen.1.16 / Dan regression
- `tools/pipeline/morph-lang.test.mjs` — unit tests (`HTd/Aamsa` false; `ANcmsd/Td` true)

Also: pack builder drops in-memory verse graphs per book and reloads gzip for hard-fail (avoids OOM thrash on full-Tanakh rebuild).

## `[aramaic-pending]` counts

| Scope | Before (v0.4.0) | After (v0.4.1) |
|-------|----------------:|---------------:|
| **Tanakh total** | **9474** | **4828** |
| Gen | 278 | 2 |
| Dan | 3648 | 3599 |
| Ezra | 1316 | 1212 |
| Jer | (inflated by adj) | 15 |

- **Removed false positives:** 9474 − 4828 = **4646** Hebrew tokens restored to SBL-Learner phonetics.
- Gen residual **2**: true Aramaic at Gen.31.47 (`יְגַר` / `שָׂהֲדוּתָא`, morph `ANp`).
- Jer residual **15**: true Aramaic verse Jer.10.11 (all morph `A…`).
- Dan/Ezra: pending == morph-starts-with-`A` counts (3599 / 1212); Hebrew sections no longer spuriously flagged.
- Gen.1.16 adjectives: `haggĕdōlîm` / `haggādōl` / `haqqāṭōn` (not pending).

## Verification
- `node --test morph-lang.test.mjs` + `oshb-w.test.mjs` green
- Hard-fail 1–8 full Tanakh / Nevi’im / Ketuvim **PASSED**
- Android `versionName` `0.4.1-poc` / `versionCode` 11
