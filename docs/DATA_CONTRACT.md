# Data contract (Sleuth / Sofer packs)

## Pack JSON (`assets/data/pack_gen_1_3.json`)

```json
{
  "meta": { "hebrew": {}, "english": {}, "phonetics": {}, "glosses": {}, "gaps": [] },
  "chapters": [{ "book": "Gen", "chapter": 1, "verseIds": ["Gen.1.1", "..."] }],
  "verses": [
    {
      "id": "Gen.1.1",
      "book": "Gen",
      "chapter": 1,
      "verse": 1,
      "english": { "text": "...", "source": "JPS 1917", "license": "Public Domain" },
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
  ],
  "glosses": {
    "H7225": {
      "id": "H7225",
      "primary": "first: beginning",
      "senses": ["..."],
      "source": "TBESH",
      "definition": "..."
    }
  }
}
```

### Hard rules

1. **Single token array** — `words[]` order is OSHB order. Never reverse Hebrew independently of phonetics.
2. **English is verse-level** — not word-aligned.
3. **יהוה** — `he` consonants only; `phonetic` = `YHWH`.
4. Gloss UI: header **Possible sense(s)**; footer **Gloss ≠ verse translation**.

### Pipeline inputs

| Input | Role |
|-------|------|
| `vendor/oshb/Gen.xml` (OSHB **v.2.2**) | Hebrew tokens, lemma, morph |
| `vendor/jps1917/engjps_002_GEN_0N_read.txt` | JPS 1917 English verses |
| `vendor/TBESH.txt` | Primary glosses |
| `vendor/HebrewStrong.xml` | Fallback glosses |
| Sofer SBL-Learner schema | Phonetics from niqqud |
