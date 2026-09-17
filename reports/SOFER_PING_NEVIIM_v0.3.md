# Sofer ping prep — Nevi’im v0.3.0-poc

- **Pack content SHA-256** (assets/data tree): `d15b92e7e5d86cdfab59f4a7d4811ddb1f0cf9b1f1ced672bdedda7f219aa97d`
- **Pack version:** 0.3.0-poc
- **Scope:** Torah + Nevi’im (Jewish order)
- **Nevi’im verses:** 9296 · **All books in catalog:** 26 · **Total verses:** 15149 · **Glosses:** 6712
- **Nevi’im K/Q:** 672 (see `reports/neviim-ketiv-qere.md`)
- **Hard-fail Nevi’im 1–8:** **PASSED** · YHWH tokens checked: 3523
- **Hard-fail full Torah+Nevi’im:** PASSED (`reports/tanakh-tn-hard-fail.json`)

## Sofer Torah HIGH baked into pipeline (all books going forward)

1. **H3071 Jehovah-nissi leak** — `GlossDisplay` no longer uses `sanitizeLine ?: primary` (Exod.17.15). Pack gloss primaries rewrite Jehovah→LORD.
2. **Pointed יהוה / H3069** — strip niqqud; `divineName=true`; phonetic `YHWH` (or MEDIUM proclitic `lYHWH` etc.).
3. **MEDIUM** — proclitic+יהוה kept on chip when present (`ליהוה` + `lYHWH`).

## Nested `<seg>` scan (Nevi’im)

| Verse | Seg type | Assert |
|-------|----------|--------|
| *(none)* | x-large | **0** in OSHB Nevi’im |
| Judg.18.30 | x-suspended | מנשה present |
| Isa.44.14 | x-small | ארן present |
| Jer.39.13 | x-small | ונבושזבן present |

Flatten via `oshb-w.mjs` used for emit + token-count hard-fail.

## Verse counts (Nevi’im)

| Book | Chapters | Verses |
|------|----------|--------|
| Joshua (Josh) | 24 | 658 |
| Judges (Judg) | 21 | 618 |
| 1 Samuel (1Sam) | 31 | 811 |
| 2 Samuel (2Sam) | 24 | 695 |
| 1 Kings (1Kgs) | 22 | 817 |
| 2 Kings (2Kgs) | 25 | 719 |
| Isaiah (Isa) | 66 | 1291 |
| Jeremiah (Jer) | 52 | 1364 |
| Ezekiel (Ezek) | 48 | 1273 |
| Hosea (Hos) | 14 | 197 |
| Joel (Joel) | 4 | 73 |
| Amos (Amos) | 9 | 146 |
| Obadiah (Obad) | 1 | 21 |
| Jonah (Jonah) | 4 | 48 |
| Micah (Mic) | 7 | 105 |
| Nahum (Nah) | 3 | 47 |
| Habakkuk (Hab) | 3 | 56 |
| Zephaniah (Zeph) | 3 | 53 |
| Haggai (Hag) | 2 | 38 |
| Zechariah (Zech) | 14 | 211 |
| Malachi (Mal) | 3 | 55 |

**Nevi’im total:** 9296 verses

## Sample cores

### Josh.1.1
- English: NOW IT came to pass after the death of Moses the servant of the LORD, that the LORD spoke unto Joshua the son of Nun, Moses' minister, sayin
- Words: 15; first: {'he': 'וַיְהִי', 'phonetic': 'wayĕhî'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}, {'he': 'יהוה', 'phonetic': 'YHWH'}]

### Isa.6.3
- English: And one called unto another, and said: Holy, holy, holy, is the LORD of hosts; the whole earth is full of His glory.
- Words: 14; first: {'he': 'וְקָרָא', 'phonetic': 'wĕqārāʾ'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

### Isa.7.14
- English: Therefore the Lord Himself shall give you a sign: behold, the young woman shall conceive, and bear a son, and shall call his name Immanuel.
- Words: 15; first: {'he': 'לָכֵן', 'phonetic': 'lākhēn'}
- YHWH tokens: []

### Jer.31.31
- English: Behold, the days come, saith the LORD, that I will make a new covenant with the house of Israel, and with the house of Judah;
- Words: 14; first: {'he': 'הִנֵּה', 'phonetic': 'hinnēh'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

### Ezek.1.1
- English: NOW IT came to pass in the thirtieth year, in the fourth month, in the fifth day of the month, as I was among the captives by the river Cheb
- Words: 17; first: {'he': 'וַיְהִי', 'phonetic': 'wayĕhî'}
- YHWH tokens: []

### Hos.1.1
- English: THE WORD of the LORD that came unto Hosea the son of Beeri, in the days of Uzziah, Jotham, Ahaz, and Hezekiah, kings of Judah, and in the da
- Words: 21; first: {'he': 'דְּבַר', 'phonetic': 'dĕvar'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

### Amos.5.24
- English: But let justice well up as waters, and righteousness as a mighty stream.
- Words: 6; first: {'he': 'וְיִגַּל', 'phonetic': 'wĕyiggal'}
- YHWH tokens: []

### Mic.6.8
- English: It hath been told thee, O man, what is good, and what the LORD doth require of thee: only to do justly, and to love mercy, and to walk humbl
- Words: 19; first: {'he': 'הִגִּיד', 'phonetic': 'higgîd'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

### Judg.18.30
- English: And the children of Dan set up for themselves the graven image; and Jonathan, the son of Gershom, the son of Manasseh, he and his sons were 
- Words: 21; first: {'he': 'וַיָּקִימוּ', 'phonetic': 'wayyāqîmû'}
- YHWH tokens: []

### Mal.3.23
- English: Behold, I will send you Elijah the prophet before the coming of the great and terrible day of the LORD.
- Words: 13; first: {'he': 'הִנֵּה', 'phonetic': 'hinnēh'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

## YHWH gloss screenshot

- Verse: Isa.6.3 · token he=`יהוה` phonetic=`YHWH` divineName=True
- Display path: GlossDisplay yhwhDisplay → primary LORD/God; definition stripped
- Artifact: `reports/yhwh-gloss-isa-6-3.png`

## Notes

- 1Kgs.22.44 = KJV 22.43!b — engjps lacks `!b` line; filled from JPS 1917 continuation (pipeline `jpsForWlc` now strips `!a/!b`).
- Checkpoint A (Former+Isa/Jer) was passed mid-run: `reports/SOFER_CHECKPOINT_NEVIIM_A.md`
