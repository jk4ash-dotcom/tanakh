# Data contract (Sleuth / Sofer packs) — v0.2

## Catalog (`assets/data/catalog.json`)

Lists books in **Jewish Tanakh order** with per-book gzip asset paths and verse counts.

## Per-book pack (`assets/data/books/{Osis}.json.gz`)

```json
{
  "meta": { "book": "Gen", "title": "Genesis", "aramaic": false, "hebrew": {}, "english": {}, "phonetics": {}, "glosses": {}, "display": {} },
  "chapters": [{ "book": "Gen", "chapter": 1, "verseIds": ["Gen.1.1", "..."] }],
  "verses": [
    {
      "id": "Gen.1.1",
      "book": "Gen",
      "chapter": 1,
      "verse": 1,
      "english": { "text": "...", "source": "JPS 1917", "license": "Public Domain", "kjvRef": "optional" },
      "words": [
        {
          "he": "בְּרֵאשִׁית",
          "lemmaId": "H7225",
          "lemmaRaw": "b/7225",
          "morph": "HR/Ncfsa",
          "phonetic": "bĕrēʾshîth",
          "glossId": "H7225",
          "divineName": false,
          "procliticNote": "optional",
          "ketiv": "optional",
          "qereFlag": false
        }
      ]
    }
  ]
}
```

Shared glosses: `assets/data/glosses.json.gz`.

### Hard rules

1. **Single token array** — `words[]` order is OSHB order. Never reverse Hebrew independently of phonetics.
2. **English is verse-level** — not word-aligned. Hebrew WLC IDs primary; `kjvRef` when VerseMap applies.
3. **יהוה** — `he` = `יהוה` (even with proclitic lemmas); `phonetic` = `YHWH`.
4. Gloss UI: header **Possible sense(s)**; footer **Gloss ≠ verse translation**.
5. **No NFC normalization** of Hebrew.
6. **Aramaic** books/spans flagged — do not silently apply Hebrew SBL-Learner.

### Pipeline inputs

| Input | Role |
|-------|------|
| `vendor/oshb/{Book}.xml` (OSHB **v.2.2**) | Hebrew tokens, lemma, morph |
| `vendor/oshb/VerseMap.xml` | WLC→KJV for engjps |
| `vendor/jps1917/engjps_*_read.txt` | JPS 1917 English verses |
| `vendor/TBESH.txt` | Primary glosses |
| `vendor/HebrewStrong.xml` | Fallback glosses |
| Sofer SBL-Learner schema | Phonetics from niqqud |
