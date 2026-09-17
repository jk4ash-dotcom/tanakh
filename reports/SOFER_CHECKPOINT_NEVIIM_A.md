# Sofer ping prep — Nevi’im checkpoint A (Former + Isa/Jer)

- **Time:** 2026-09-17 18:33 ET
- **Pack version target:** 0.3.0-poc
- **Status:** READY FOR REVIEW — Ezek + Twelve generating now
- **Books ready:** Josh, Judg, 1Sam, 2Sam, 1Kgs, 2Kgs, Isa, Jer (8 books · 6973 verses)
- **YHWH divineName tokens in ready packs:** 2582
- **K/Q flags in ready packs:** 511
- **HIGH (1):** GlossDisplay H3071 sanitize fallback fixed (no `?: primary` re-leak)
- **HIGH (2):** H3069/pointed יהוה post-patched on ready packs; live in pipeline for Ezek+
- **Nested segs flatten:** Judg.18.30 has מנשה; Isa.44.14 / Jer.39.13 nested x-small present in pack token counts
- **x-large in Nevi’im OSHB:** **0**

## Verse counts

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

## Sample cores

### Josh.1.1
- English: NOW IT came to pass after the death of Moses the servant of the LORD, that the LORD spoke unto Joshua the son of Nun, Moses' minister, sayin
- Words: 15; first: {'he': 'וַיְהִי', 'phonetic': 'wayĕhî'}
- YHWH: [{'he': 'יהוה', 'phonetic': 'YHWH'}, {'he': 'יהוה', 'phonetic': 'YHWH'}]
- Cons: ['ויהי', 'אחרי', 'מות', 'משה', 'עבד', 'יהוה', 'ויאמר', 'יהוה', 'אל', 'יהושע', 'בן', 'נון', 'משרת', 'משה', 'לאמר']

### Isa.6.3
- English: And one called unto another, and said: Holy, holy, holy, is the LORD of hosts; the whole earth is full of His glory.
- Words: 14; first: {'he': 'וְקָרָא', 'phonetic': 'wĕqārāʾ'}
- YHWH: [{'he': 'יהוה', 'phonetic': 'YHWH'}]
- Cons: ['וקרא', 'זה', 'אל', 'זה', 'ואמר', 'קדוש', 'קדוש', 'קדוש', 'יהוה', 'צבאות', 'מלא', 'כל', 'הארץ', 'כבודו']

### Isa.7.14
- English: Therefore the Lord Himself shall give you a sign: behold, the young woman shall conceive, and bear a son, and shall call his name Immanuel.
- Words: 15; first: {'he': 'לָכֵן', 'phonetic': 'lākhēn'}
- YHWH: []
- Cons: ['לכן', 'יתן', 'אדני', 'הוא', 'לכם', 'אות', 'הנה', 'העלמה', 'הרה', 'וילדת', 'בן', 'וקראת', 'שמו', 'עמנו', 'אל']

### Jer.31.31
- English: Behold, the days come, saith the LORD, that I will make a new covenant with the house of Israel, and with the house of Judah;
- Words: 14; first: {'he': 'הִנֵּה', 'phonetic': 'hinnēh'}
- YHWH: [{'he': 'יהוה', 'phonetic': 'YHWH'}]
- Cons: ['הנה', 'ימים', 'באים', 'נאם', 'יהוה', 'וכרתי', 'את', 'בית', 'ישראל', 'ואת', 'בית', 'יהודה', 'ברית', 'חדשה']

### Judg.18.30
- English: And the children of Dan set up for themselves the graven image; and Jonathan, the son of Gershom, the son of Manasseh, he and his sons were 
- Words: 21; first: {'he': 'וַיָּקִימוּ', 'phonetic': 'wayyāqîmû'}
- YHWH: []
- Cons: ['ויקימו', 'להם', 'בני', 'דן', 'את', 'הפסל', 'ויהונתן', 'בן', 'גרשם', 'בן', 'מנשה', 'הוא', 'ובניו', 'היו', 'כהנים', 'לשבט', 'הדני', 'עד', 'יום', 'גלות', 'הארץ']

### Isa.44.14
- English: He heweth him down cedars, and taketh the ilex and the oak, and strengtheneth for himself one among the trees of the forest; he planteth a b
- Words: 14; first: {'he': 'לִכְרָת', 'phonetic': 'likhrāth'}
- YHWH: []
- Cons: ['לכרת', 'לו', 'ארזים', 'ויקח', 'תרזה', 'ואלון', 'ויאמץ', 'לו', 'בעצי', 'יער', 'נטע', 'ארן', 'וגשם', 'יגדל']

### Jer.39.13
- English: So Nebuzaradan the captain of the guard sent, and Nebushazban Rab-saris, and Nergal-sarezer Rab-mag, and all the chief officers of the king 
- Words: 16; first: {'he': 'וַיִּשְׁלַח', 'phonetic': 'wayyishlaḥ'}
- YHWH: []
- Cons: ['וישלח', 'נבוזראדן', 'רב', 'טבחים', 'ונבושזבן', 'רב', 'סריס', 'ונרגל', 'שר', 'אצר', 'רב', 'מג', 'וכל', 'רבי', 'מלך', 'בבל']

## Remaining

- Ezek + Hos–Mal
- Full neviim hard-fail + K/Q + SOFER_PING_NEVIIM_v0.3.md + APK
