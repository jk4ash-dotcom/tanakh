# Source pins (v0.2 Torah / full-corpus ready)

| Source | Pin | Path / URL |
|--------|-----|------------|
| OSHB / morphhb WLC | **v.2.2** (tag `v.2.2`, commit `6a5db284…`) | `vendor/oshb/*.xml` from morphhb zip; sha256 in `vendor/_fetch/morphhb-v.2.2.zip.sha256` |
| OSHB VerseMap | v.2.2 `wlc/VerseMap.xml` | `vendor/oshb/VerseMap.xml` — WLC↔KJV for engjps lookup |
| JPS 1917 (PD) | ebible.org engjps readaloud (zip fetched 2026-09-17) | `vendor/jps1917/engjps_*_*_read.txt (Torah+Nevi'im+Ketuvim)` |
| TBESH | STEPBible-Data (CC BY 4.0) | `vendor/TBESH.txt` |
| HebrewStrong.xml | openscriptures/HebrewLexicon (CC BY 4.0) | `vendor/HebrewStrong.xml` |
| hebrew-transliteration | npm 2.11.0 (MIT) | `tools/pipeline` |

**Nav order:** Jewish Tanakh (`tools/pipeline/books.mjs` `JEWISH_TANAKH_ORDER`), not Christian/filename order.

Do not substitute UXLC for Hebrew tokens. Do not NFC-normalize Hebrew. Phonetics from OSHB niqqud via Sofer SBL-Learner — never from English. Biblical Aramaic (Dan/Ezra) must not silently use Hebrew-only rules.
