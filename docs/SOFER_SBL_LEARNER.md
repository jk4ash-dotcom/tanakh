# Sofer SBL-Learner phonetic mapping

Phonetics are generated from **OSHB / WLC niqqud** with
[`hebrew-transliteration`](https://github.com/charlesLoder/hebrew-transliteration) (MIT)
using the **Sofer SBL-Learner** schema (`tools/pipeline/sofer-sbl-learner.mjs`).

They are **never** derived from English verse text.

## Policy

| Topic | Rule |
|-------|------|
| Tradition | Biblical / Tiberian (not Modern Israeli) |
| Digraphs | `sh` (שׁ), `kh` (soft כ/ך), `ts` (צ), `ʾ` (א), `ʿ` (ע) |
| Vocal shewa | `ĕ` (not ə; not silent Modern reduction) |
| Hateph | `ĕ` / `ă` / `ŏ` |
| Divine name | Surface **יהוה**; phonetic **YHWH** only — no invented vocalization |
| Ketiv / qere | Phonetics follow **qere**; ketiv retained as flag when present |
| Prefixes | Stay on the surface token; gloss may note proclitic separately |
| English row | JPS 1917 verse text may say God / Lord / LORD independently |

## Grapheme → Latin (summary)

### Consonants

| Hebrew | Learner | Notes |
|--------|---------|-------|
| א | ʾ | |
| בּ / ב | b / v | spirantization |
| גּ / ג | g / g | |
| דּ / ד | d / d | |
| ה | h | |
| ו | w | |
| ז | z | |
| ח | ḥ | underdot (not `ch`) |
| ט | ṭ | |
| י | y | |
| כּ / כ / ך | k / kh / kh | digraph `kh` for soft |
| ל | l | |
| מ / ם | m | |
| נ / ן | n | |
| ס | s | |
| ע | ʿ | |
| פּ / פ / ף | p / f / f | |
| צ / ץ | ts | digraph |
| ק | q | |
| ר | r | |
| שׁ / שׂ | sh / s | digraph `sh` |
| תּ / ת | t / th | soft tav `th` (not Modern `t`) |

### Vowels

| Mark | Learner |
|------|---------|
| Shewa (vocal) | ĕ |
| Ḥateph segol / pataḥ / qamats | ĕ / ă / ŏ |
| Hiriq | i (î with yod) |
| Tsere | ē (ê with yod) |
| Segol | e |
| Pataḥ | a (furtive pataḥ a) |
| Qamats gadol | ā |
| Qamats qatan | o |
| Holam | ō (ô with vav) |
| Qubuts / shureq | u / û |

Syllabification options follow SBL defaults useful for Biblical Hebrew
(`qametsQatan`, `longVowels`, `sqnmlvy`, `shevaAfterMeteg`, etc.).

## Rebuild

```bash
cd tools/pipeline && npm install && npm run build
```

## Biblical Aramaic

Daniel and Ezra contain Biblical Aramaic. Do **not** silently apply this Hebrew SBL-Learner schema to those spans. **v0.4.0-poc Sofer-approved path:** detect OSHB morph language `A*`, set `aramaic=true`, phonetic `[aramaic-pending]` (no Hebrew SBL). Hebrew morph tokens in Dan/Ezra still use this schema. Torah has no Aramaic body text.
