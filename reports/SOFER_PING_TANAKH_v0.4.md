# Sofer ping — Full Tanakh v0.4.0-poc

- **Pack content SHA-256** (assets/data tree): `825a54dcadadc190665732f1856e897f31cb7612acc57881b64fedbe569fc310`
- **Pack version:** 0.4.0-poc
- **Scope:** Full Tanakh (Torah + Nevi'im + Ketuvim)
- **Books:** 39 · **Verses:** 23213 · **Glosses:** 9200 · **K/Q:** 1245
- **By division:** Torah 5853 · Nevi’im 9296 · Ketuvim 8064
- **Hard-fail full Tanakh 1–8:** **PASSED** (`reports/tanakh-hard-fail.json`)
- **Hard-fail Nevi’im:** **PASSED** · **Ketuvim:** **PASSED**
- **YHWH tokens (tanakh hard-fail sample):** 6828

## Priority A — Nevi’im orphan-qere HIGH ×8 (fixed)

Root cause: OSHB “Adaptations to a Qere which L and BHS do not indicate” —
`<rdg type="x-qere">` with **no** preceding `x-ketiv`. Prior extractor skipped these.
Fix in `tools/pipeline/oshb-w.mjs`: insert orphan qere tokens; `catchWord` (Jer.48.44) replaces surface + stores ketiv.
Also covers Ruth.3.5 / Ruth.3.17 (`אלי`).

| Verse | Expected cons | Pack count | Present |
|-------|---------------|------------|---------|
| Judg.20.13 | בני | 21 | **yes** |
| 2Sam.8.3 | פרת | 13 | **yes** |
| 2Sam.16.23 | איש | 19 | **yes** |
| 2Kgs.19.31 | צבאות | 12 | **yes** |
| 2Kgs.19.37 | בניו | 20 | **yes** |
| Jer.31.38 | באים | 12 | **yes** |
| Jer.48.44 | הנס | 20 | **yes** |
| Jer.50.29 | לה | 29 | **yes** |

### Sequence checks
- Judg.20.13: `אבו→בני→בנימן`
- Jer.50.29: `יהי→לה→פלטה`
- Jer.48.44: surface `הנס` ketiv `הניס` qereFlag=True

## Priority B — Ketuvim / Prov JPS

- Fixed `titleHints` for Prov: `/^The Proverbs\.?$/i` so verse-1 “THE PROVERBS of Solomon…” is not skipped.
- Prov.1.33 / Prov.10.32 English present; Ketuvim hard-fail **PASSED**.
- Dan/Ezra: `aramaic: true` on catalog + book meta; Aramaic morph tokens use `[aramaic-pending]`.

## Sample cores (Ketuvim)
### Ps.1.1
- English: BOOK I HAPPY IS the man that hath not walked in the counsel of the wicked, nor stood in the way of sinners, nor sat in t
- Words: 15; first: {'he': 'אַשְׁרֵי', 'phonetic': 'ʾashrê'}
- YHWH tokens: []

### Prov.1.1
- English: THE PROVERBS of Solomon the son of David, king of Israel;
- Words: 6; first: {'he': 'מִשְׁלֵי', 'phonetic': 'mishlê'}
- YHWH tokens: []

### Prov.1.33
- English: But whoso hearkeneth unto me shall dwell securely, and shall be quiet without fear of evil.'
- Words: 7; first: {'he': 'וְשֹׁמֵעַ', 'phonetic': 'wĕshōmēaʿ'}
- YHWH tokens: []

### Prov.10.32
- English: The lips of the righteous know what is acceptable; but the mouth of the wicked is all frowardness.
- Words: 7; first: {'he': 'שִׂפְתֵי', 'phonetic': 'sifthê'}
- YHWH tokens: []

### Job.1.1
- English: THERE was a man in the land of Uz, whose name was Job; and that man was whole-hearted and upright, and one that feared G
- Words: 15; first: {'he': 'אִישׁ', 'phonetic': 'ʾîsh'}
- YHWH tokens: []

### Ruth.1.1
- English: AND IT came to pass in the days when the judges judged, that there was a famine in the land. And a certain man of Beth-l
- Words: 19; first: {'he': 'וַיְהִי', 'phonetic': 'wayĕhî'}
- YHWH tokens: []

### Ruth.3.5
- English: And she said unto her: 'All that thou sayest unto me I will do.'
- Words: 7; first: {'he': 'וַתֹּאמֶר', 'phonetic': 'wattōʾmer'}
- YHWH tokens: []
- Cons includes אלי: True

### Dan.2.4
- English: Then spoke the Chaldeans to the king in Aramaic: 'O king, live for ever! tell thy servants the dream, and we will declar
- Words: 12; first: {'he': 'וַיְדַבְּרוּ', 'phonetic': 'wayĕdabbĕrû'}
- YHWH tokens: []

### Ezra.4.8
- English: Rehum the commander and Shimshai the scribe wrote a letter against Jerusalem to Artaxerxes the king in this sort —
- Words: 13; first: {'he': 'רְחוּם', 'phonetic': '[aramaic-pending]'}
- YHWH tokens: []

### 1Chr.1.1
- English: ADAM, SETH, Enosh;
- Words: 3; first: {'he': 'אָדָם', 'phonetic': 'ʾādām'}
- YHWH tokens: []

## Sample cores (Nevi’im 8-fix verify)
### Judg.20.13
- English: Now therefore deliver up the men, the base fellows that are in Gibeah, that we may put them to death, and put away evil 
- Words: 21; cons: ['ועתה', 'תנו', 'את', 'האנשים', 'בני', 'בליעל', 'אשר', 'בגבעה', 'ונמיתם', 'ונבערה', 'רעה', 'מישראל', 'ולא', 'אבו', 'בני', 'בנימן', 'לשמע', 'בקול', 'אחיהם', 'בני', 'ישראל']

### 2Sam.8.3
- English: David smote also Hadadezer the son of Rehob, king of Zobah, as he went to establish his dominion at the river Euphrates.
- Words: 13; cons: ['ויך', 'דוד', 'את', 'הדדעזר', 'בן', 'רחב', 'מלך', 'צובה', 'בלכתו', 'להשיב', 'ידו', 'בנהר', 'פרת']

### 2Sam.16.23
- English: Now the counsel of Ahithophel, which he counselled in those days, was as if a man inquired of the word of God; so was al
- Words: 19; cons: ['ועצת', 'אחיתפל', 'אשר', 'יעץ', 'בימים', 'ההם', 'כאשר', 'ישאל', 'איש', 'בדבר', 'האלהים', 'כן', 'כל', 'עצת', 'אחיתפל', 'גם', 'לדוד', 'גם', 'לאבשלם']

### 2Kgs.19.31
- English: For out of Jerusalem shall go forth a remnant, and out of mount Zion they that shall escape; the zeal of the LORD of hos
- Words: 12; cons: ['כי', 'מירושלם', 'תצא', 'שארית', 'ופליטה', 'מהר', 'ציון', 'קנאת', 'יהוה', 'צבאות', 'תעשה', 'זאת']

### 2Kgs.19.37
- English: And it came to pass, as he was worshipping in the house of Nisroch his god, that Adrammelech and Sarezer his sons smote 
- Words: 20; cons: ['ויהי', 'הוא', 'משתחוה', 'בית', 'נסרך', 'אלהיו', 'ואדרמלך', 'ושראצר', 'בניו', 'הכהו', 'בחרב', 'והמה', 'נמלטו', 'ארץ', 'אררט', 'וימלך', 'אסר', 'חדן', 'בנו', 'תחתיו']

### Jer.31.38
- English: Behold, the days come, saith the LORD, that the city shall be built to the LORD from the tower of Hananel unto the gate 
- Words: 12; cons: ['הנה', 'ימים', 'באים', 'נאם', 'יהוה', 'ונבנתה', 'העיר', 'ליהוה', 'ממגדל', 'חננאל', 'שער', 'הפנה']

### Jer.48.44
- English: He that fleeth from the terror shall fall into the pit; and he that getteth up out of the pit shall be taken in the trap
- Words: 20; cons: ['הנס', 'מפני', 'הפחד', 'יפל', 'אל', 'הפחת', 'והעלה', 'מן', 'הפחת', 'ילכד', 'בפח', 'כי', 'אביא', 'אליה', 'אל', 'מואב', 'שנת', 'פקדתם', 'נאם', 'יהוה']

### Jer.50.29
- English: Call together the archers against Babylon, all them that bend the bow; encamp against her round about, let none thereof 
- Words: 29; cons: ['השמיעו', 'אל', 'בבל', 'רבים', 'כל', 'דרכי', 'קשת', 'חנו', 'עליה', 'סביב', 'אל', 'יהי', 'לה', 'פלטה', 'שלמו', 'לה', 'כפעלה', 'ככל', 'אשר', 'עשתה', 'עשו', 'לה', 'כי', 'אל', 'יהוה', 'זדה', 'אל', 'קדוש', 'ישראל']

## Notes
- Prefer single **v0.4.0-poc** full Tanakh ship (Torah+Nevi’im+Ketuvim); Nevi’im 8-fix folded in.
- MEDIUM proclitic+YHWH (`ליהוה` / `lYHWH`) already in pipeline + PackSanityTest.
- Generated: 2026-09-18T05:41:39.478175+00:00
