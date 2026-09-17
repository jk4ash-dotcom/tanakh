# Sofer ping prep — Torah v0.2.1-poc (HOTFIX)

- **Pack content SHA-256** (assets/data tree): `53ecaa1a424dc069aa8bd02dcea1158a951ab800c30c886424a13c80409050bf`
- **Pack version:** 0.2.1-poc
- **Scope:** Torah (Gen–Deut) — Nevi’im **paused** (not in this run)
- **Verses:** 5853 · **Glosses:** 3532 · **K/Q:** 67
- **Hard-fail:** PASSED · YHWH tokens checked: 1815
- **K/Q report:** `reports/torah-ketiv-qere.md` (67 entries)

## Hotfix: nested OSHB `<seg type="x-large">` inside `<w>`

Pack emit previously used `([^<]*)` for `<w>` inner text, which **dropped** words whose surface is split by a nested large-letter `<seg>` (hard-fail token-count did not catch it).

**Fix:** flatten all text nodes under `<w>` (ignore seg tags for surface form) via `tools/pipeline/oshb-w.mjs`; rebuild Torah; hard-fail re-parses OSHB with the **same** flatten and asserts known x-large verses.

| Verse | Before | After | Restored |
|-------|--------|-------|----------|
| Deut.6.4 | 4 tokens | **6** | שְׁמַע, אֶחָד |
| Lev.11.42 | missing גָּחוֹן | **22** tokens | גָּחוֹן |
| Num.27.5 | missing מִשְׁפָּטָן | **6** tokens | מִשְׁפָּטָן |

## Verse counts per book

| Book | Chapters | Verses |
|------|----------|--------|
| Genesis (Gen) | 50 | 1533 |
| Exodus (Exod) | 40 | 1213 |
| Leviticus (Lev) | 27 | 859 |
| Numbers (Num) | 36 | 1289 |
| Deuteronomy (Deut) | 34 | 959 |

## Sample verses (for Sofer core list)

### Gen.1.1
- English: IN THE beginning God created the heaven and the earth.
- Words: 7; first: {'he': 'בְּרֵאשִׁית', 'phonetic': 'bĕrēʾshîth'}
- YHWH tokens: []

### Gen.2.4
- English: These are the generations of the heaven and of the earth when they were created, in the day that the LORD God made earth
- Words: 11; first: {'he': 'אֵלֶּה', 'phonetic': 'ʾēlleh'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

### Exod.3.14
- English: And God said unto Moses: 'I AM THAT I AM'; and He said: 'Thus shalt thou say unto the children of Israel: I AM hath sent
- Words: 15; first: {'he': 'וַיֹּאמֶר', 'phonetic': 'wayyōʾmer'}
- YHWH tokens: []

### Exod.3.15
- English: And God said moreover unto Moses: 'Thus shalt thou say unto the children of Israel: The LORD, the God of your fathers, t
- Words: 28; first: {'he': 'וַיֹּאמֶר', 'phonetic': 'wayyōʾmer'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

### Lev.19.18
- English: Thou shalt not take vengeance, nor bear any grudge against the children of thy people, but thou shalt love thy neighbour
- Words: 12; first: {'he': 'לֹא', 'phonetic': 'lōʾ'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

### Num.6.24
- English: The LORD bless thee, and keep thee;
- Words: 3; first: {'he': 'יְבָרֶכְךָ', 'phonetic': 'yĕvārekhkhā'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

### Deut.6.4
- English: HEAR, O ISRAEL: THE LORD OUR GOD, THE LORD IS ONE.
- Words: 6; first: {'he': 'שְׁמַע', 'phonetic': 'shĕmaʿ'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}, {'he': 'יהוה', 'phonetic': 'YHWH'}]

### Deut.6.5
- English: And thou shalt love the LORD thy God with all thy heart, and with all thy soul, and with all thy might.
- Words: 10; first: {'he': 'וְאָהַבְתָּ', 'phonetic': 'wĕʾāhavtā'}
- YHWH tokens: [{'he': 'יהוה', 'phonetic': 'YHWH'}]

## Deut.6.4 chips (post-fix)

- Word count: **6**
- Surfaces: ['שְׁמַע', 'יִשְׂרָאֵל', 'יהוה', 'אֱלֹהֵינוּ', 'יהוה', 'אֶחָד']
- Consonants: ['שמע', 'ישראל', 'יהוה', 'אלהינו', 'יהוה', 'אחד']
- Screenshot: `reports/deut-6-4-chips.png`

## YHWH gloss screenshot

Prior artifact still valid: `reports/yhwh-gloss-gen-2-4.png` (Gen.2.4 divine-name sheet).

## Gaps note

Non-fatal gap notes: see `reports/torah-gaps.json` (count=144). Hard-fail still passed.

