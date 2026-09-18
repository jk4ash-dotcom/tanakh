# Sofer ping — Ketuvim nested-seg + proclitic gloss (v0.6.0-template-poc)

## Gates (1)

Ketuvim nested `<seg>` hard-fail (surfaces already in packs; gated like Torah/Nevi’im):

| Verse | Type | mustIncludeCons | Result |
|-------|------|-----------------|--------|
| Job.38.13 | x-suspended | רשעים | PASS |
| Job.38.15 | x-suspended | מרשעים | PASS |
| Ps.80.14 | x-suspended | מיער | PASS |
| Prov.16.28 | x-small | ונרגן | PASS |

- `reports/ketuvim-hard-fail.json` — **PASSED**
- `reports/tanakh-hard-fail.json` — **PASSED**
- Runner: `node tools/pipeline/run-hard-fail.mjs`

## Proclitic compound gloss (2) — Sofer constraints

| lemmaRaw | count | treatment |
|----------|------:|-----------|
| c/l | 74 | functional `pfx:c/l` |
| c/b | 16 | functional `pfx:c/b` |
| c/m | 6 | functional `pfx:c/m` |
| s/l | 2 | functional `pfx:s/l` |
| i/l | 1 | functional `pfx:i/l` |

- **Before:** 99 gloss gaps (`(no lemma)` / UI “No gloss”)
- **After:** 99/99 `source=proclitic-functional` (0 compound gaps)
- Policy: functional role line from procliticNote — **not** fake TBESH content; YHWH/DN sanitize untouched; real numeric lemmas unchanged
- Docs: `docs/DATA_CONTRACT.md` § Proclitic-only compound lemmas
- Small patch (no full `--scope all`): `tools/pipeline/patch-proclitic-gloss.mjs`

## Critic MEDIUM (3)

- Per-book `verseId → Verse` + chapter lists in `PackRepository`
- Gloss cold-start: catalog-first load; `ensureGlosses()` warm in background + before VerseScreen

## HOLD

- Aramaic SBL — not started; `[aramaic-pending]` on true A… morph remains template-acceptable
