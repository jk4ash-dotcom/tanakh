# Word chips

Word chips show each OSHB token as a vertical pair (Hebrew chip over phonetic chip) in a single LTR row so an RTL locale cannot reverse token order.

## Sub-features

- `chips-order` keeps `verse.words[]` / `GlossDisplay.displayTokens` order (never naïve-reverse Hebrew).
- `chips-pair` renders Hebrew (`Noto Sans Hebrew`, RTL text direction on the glyph) above Sofer SBL-Learner phonetics.
- `chips-yhwh` shows divine-name tokens as `יהוה` / `YHWH` (proclitic example `ליהוה` / `laYHWH` on `Gen.4.3` in the fixture).
- `chips-ltr` forces `LocalLayoutDirection.Ltr` on the chip `Row` (`VerseScreen`).

## How to get to it (user POV)

- Open any verse (see [Verse browse](./verse-browse.md)).
- Read the legend `Hebrew + phonetic (OSHB order, LTR paired chips)`.
- Scroll the chip row horizontally if needed. Tap either chip of a pair (both set `selected` to that token).

## Driving it with verify-tanakh

Preconditions:

- Verse `Gen.1.1` is on screen (Compose) **or** `PackSanityTest` is the JVM plane.
- Do not assert chip order from a screenshot alone if the dump has no token text.

- **JVM order (preferred without emulator).** Run `verify-tanakh drive pack-sanity`. Require `gen11_tokenOrderAndPhonetics` (7 tokens, first `בְּרֵאשִׁית`/`bĕrēʾshîth`, last `הָאָרֶץ`/`hāʾārets`) and `ltr_order_invariant_displayTokensNeverReverses` (display list equals `v.words`).
- **JVM YHWH.** Same Drive: `yhwh_hardRule_consonantsAndPhonetic` on `Gen.2.4` / `Gen.4.3` / `Exod.3.15` / `Deut.6.4`.
- **Compose row.** After `emulator-smoke` reaches `04-verse.xml`, require text `bĕrēʾshîth` and `בְּרֵאשִׁית`. If both `בְּרֵאשִׁית` and `הָאָרֶץ` appear, the first must precede the last in dump document order (`uiautomator_drive.py`).
- **Proof.** JVM: JUnit XML for the tests above. UI: `04-verse.xml` plus the legend string. Record plane `jvm` vs `emulator`.

## Gotchas

- Compose may omit Hebrew from uiautomator text. Prefer the Latin phonetic `bĕrēʾshîth` (character `ĕ` U+0115, `ʾ` U+02BE). A missing dump is an emulator-path fail, not a reason to cite only JUnit.
- Do not reverse the verse string to "fix" RTL. The contract (`docs/DATA_CONTRACT.md`) is a single `words[]` array.
- Phonetics are Sofer SBL-Learner (Biblical/Tiberian), not Modern Israeli. `PackSanityTest` is the oracle for fixture verses.
- Ketiv/qere: phonetic follows qere; sheet may show `Ketiv (written): … — phonetic follows qere` (`Gen.8.17` in the fixture).
- Qumran `Q` markers sit **under** chips (`DssVariantWordMarker`), not inside the Hebrew AssistChip. They are absent unless a shipped DSS note anchors that word (e.g. `Isa.53.11` wordIndex 2).
