# DSS variant notes — Phase-1 stub

**Status:** scaffolding only. Content is **pending Sofer**. Not a feature release.
**Base text:** OSHB / WLC remains the sole displayed Hebrew. DSS notes are optional
learner annotations; they never replace Masoretic/OSHB tokens.

## Product rules (Sleuth-locked)

| Rule | Detail |
|------|--------|
| Offline-first | Asset pack only. No network. No DSS image fetch. |
| No redistribute | Do **not** ship or re-export ETCBC / DSS corpus dumps. Curated notes only (~100). |
| No IAA images | No scroll photographs, fragment viewers, or image URLs. |
| No DSS Bible English | Do not paste “DSS Bible” English translations as the note body. |
| Sense-changing | Prefer `plus` / `minus` / `lexeme` / `literary` / `language_seam`. Orthography-only is out of Phase-1 defaults (`advanced` / future). |
| Gate UI | Show chrome only when `ship: true` **and** pack has ≥1 such note. Stub seeds use `ship: false`. |
| About one-liner | “Optional Qumran readings are notes only; they do not replace the Masoretic/OSHB text.” |

### Out of scope (Phase-1)

- Esther (no DSS witness of MT Esther)
- Apocrypha / deuterocanonical additions as primary nav
- Sectarian / community scrolls (Rule, War, Hodayot, etc.) as “biblical variants”
- Full apparatus, parallel columns, or critical edition UX

## Attach keys (Sofer-locked)

| Key | Rule |
|-----|------|
| `osis` | Equals `Verse.id` (`Book.chapter.verse`, e.g. `Isa.53.11`). |
| `oshbBook` / `chapter` / `verse` | Must match pack verse. |
| Word | `anchor.type = "word"` + `wordIndex` (0-based into `Verse.words`, LTR chips). Optional `wordIndexEnd` for spans. |
| Snapshot | For `type=word`, `he` + `lemmaId` from that token; **hard-fail** on drift. |
| Verse / literary / seam | `anchor.type = "verse"`. |
| `oshbWordIds` | Optional; **ignore** until pack exposes ids. Do not require for Phase-1. |

### Example (Isa.53.11)

Index `2` = `יִרְאֶה` / `H7200`:

```json
"anchor": { "type": "word", "wordIndex": 2, "he": "יִרְאֶה", "lemmaId": "H7200" }
```

## Placement (derived in loader — no JSON field)

| Condition | Placement |
|-----------|-----------|
| `anchor.type == "word"` | `word_chip` |
| `anchor.type == "verse"` && `category == "language_seam"` | `seam_marker` |
| `anchor.type == "verse"` && `category == "literary"` && book-level note* | `book_banner` |
| else verse-level | `verse_indicator` |

\*Book-level literary: `verse == 0` or `osis` ends with `.0` / explicit whole-book target; Phase-1 samples use verse-level literary → `verse_indicator`.

## Pack location

| Path | Role |
|------|------|
| `app/src/main/assets/data/dss_variants.json` | Primary |
| `app/src/main/assets/data/dss_variants.json.gz` | Optional gzip twin |

Missing asset → empty pack → **zero UI chrome**.

## Schema (FINAL LOCK — do not fork)

```json
{
  "version": "0.1-stub",
  "status": "SAMPLE — not Sofer-signed",
  "notes": [
    {
      "id": "dss-isa-53-11-a",
      "osis": "Isa.53.11",
      "oshbBook": "Isa",
      "chapter": 53,
      "verse": 11,
      "anchor": {
        "type": "word",
        "wordIndex": 2,
        "he": "יִרְאֶה",
        "lemmaId": "H7200"
      },
      "priority": 1,
      "category": "plus",
      "mss": ["1QIsa_a"],
      "mtSummary": "SAMPLE placeholder — Sofer fills MT contrast.",
      "dssHebrew": "",
      "dssSummary": "SAMPLE: learner one-liner — Sofer fills consonants + gloss.",
      "uxLabel": "Qumran reading",
      "disclaimer": "Does not replace the Masoretic/OSHB text",
      "refs": ["Ulrich BQS p.__", "DJD ___"],
      "ship": false,
      "advanced": false
    }
  ]
}
```

### Fields

| Field | Required | Notes |
|-------|----------|-------|
| `id` | yes | Stable (`dss-…`) |
| `osis` | yes | `Verse.id` |
| `oshbBook` / `chapter` / `verse` | yes | Match pack |
| `anchor.type` | yes | `word` \| `verse` |
| `anchor.wordIndex` | when word | 0-based |
| `anchor.wordIndexEnd` | no | Inclusive end for spans |
| `anchor.he` / `anchor.lemmaId` | when word | Hard-fail drift check |
| `anchor.oshbWordIds` | no | Ignore until pack has ids |
| `priority` | no | Lower = higher rank |
| `category` | yes | `plus` \| `minus` \| `lexeme` \| `literary` \| `language_seam` |
| `mss` | yes | Sigla array |
| `mtSummary` | no | Short MT contrast |
| `dssHebrew` | yes* | Consonants; `""` OK in SAMPLE |
| `dssSummary` | yes | One learner sentence |
| `uxLabel` | no | Default `"Qumran reading"` |
| `disclaimer` | no | Default below |
| `refs` | no | BQS / DJD citations only |
| `ship` | yes | UI gate |
| `advanced` | no | Hidden by default |

### Fixed UX copy

Sheet: **dssHebrew · mss · dssSummary · disclaimer**

> Does not replace the Masoretic/OSHB text

## How Sofer fills the pack

1. Edit `dss_variants.json` (or `.json.gz`).
2. Join on `osis` = `Verse.id`; word notes: `wordIndex` + `he`/`lemmaId` snapshot.
3. Fill `dssHebrew`, `dssSummary`, `mtSummary`, `refs`; set `ship: true` when approved.
4. ~100 sense-changing notes; no images / ETCBC dumps / DSS-Bible English.
5. Hard-fail: `Verse.words[wordIndex].he` and `.lemmaId` match snapshot.

## App wiring

- `DssVariantRepository` — IO; gz/plain candidates; missing → empty.
- Visible = `ship == true` only (SAMPLE seeds → no chrome).
- Placement derived at load (see table).
- Verse screen: subtle indicator when visible notes exist → bottom sheet.

## SAMPLE seeds (`ship: false`)

| id | osis | anchor | category |
|----|------|--------|----------|
| `dss-isa-53-11-a` | Isa.53.11 | word @ 2 יִרְאֶה/H7200 | plus |
| `dss-deut-32-8-a` | Deut.32.8 | verse | lexeme |
| `dss-deut-32-43-a` | Deut.32.43 | verse | literary |
| `dss-ps-145-nun-a` | Ps.145.13 | verse | plus |
| `dss-dan-seam-a` | Dan.2.4 | verse | language_seam |
| `dss-jer-edition-a` | Jer.10.4 | verse | literary |

Not Sofer-signed — not apparatus.
