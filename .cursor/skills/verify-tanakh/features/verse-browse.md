# Verse browse

Verse browse lets a user walk Jewish Tanakh order from the Home book list through a book's chapters to a verse page that shows Hebrew chips and the JPS 1917 English row.

## Sub-features

- `browse-home` shows catalog scope and Torah / Nevi'im / Ketuvim headers after catalog load.
- `browse-book` opens a book pack and lists `BookTitles.chapterLabel` cards.
- `browse-chapter` lists `chapter:verse` cards with a two-line JPS preview.
- `browse-verse` opens `verse/{verseId}` and shows title `BookTitles.verseLabel`.

## How to get to it (user POV)

- Launch the app (`MainActivity`). Wait until Home title `Tanakh Learner` replaces `Loading Tanakh catalog…`.
- Tap a book card titled from the catalog (first Torah card is `Genesis`).
- Tap a chapter card (`Genesis 1`).
- Tap a verse card (`1:1`).
- Back uses the `Back` icon on Book, Chapter, Verse, and About.

## Driving it with verify-tanakh

Preconditions:

- Doctor is healthy.
- Compose path: `launch --mode emulator` printed `READY (emulator)` and Home dump contains `Tanakh Learner`.
- JVM substitute: `drive pack-sanity` is allowed only for catalog order + sample verse presence — mark Compose entry points **skipped**.

- **Open Home.** Confirm title `Tanakh Learner` and a scope line (`catalog.scope`, shipped as `Full Tanakh (Torah + Nevi'im + Ketuvim)`). Division headers `Torah`, `Nevi'im`, `Ketuvim` appear in that order (`HomeScreen` `divisions` list).
- **Open Genesis.** Tap text `Genesis`. Run `verify-tanakh drive emulator-smoke` (first hop) or tap via uiautomator. App bar becomes `Genesis`. A loading line `Loading Genesis…` may appear while `ensureBook("Gen")` runs.
- **Open chapter 1.** Tap text `Genesis 1`. App bar becomes `Genesis 1`.
- **Open verse 1:1.** Tap text `1:1`. App bar becomes `Genesis 1:1`. Body includes `Hebrew + phonetic (OSHB order, LTR paired chips)` and `JPS 1917 (verse)`.
- **JVM catalog order.** Run `verify-tanakh drive pack-sanity`. `PackSanityTest.catalog_jewishOrder_torahBooks` and doctor catalog checks prove `Gen` first / `2Chr` last / `Dan` in Writings. This does **not** prove Compose navigation.
- **Proof.** UI: keep `emulator-smoke/01-home.xml` through `04-verse.xml` showing those strings. JVM: JUnit XML for `sampleVerses_perTorahBook_present` (`Gen.1.1`, `Exod.3.14`, `Lev.19.18`, `Num.6.24`, `Deut.6.4`).

## Gotchas

- There is no `verse/Gen.1.1` intent filter. `adb shell am start` with a custom URI will not open a verse.
- Book OSIS ids (`Gen`, `Exod`, `1Sam`) are navigation keys, not Home card titles. Cards show `Genesis`, `Exodus`, `1 Samuel`.
- Daniel is under Ketuvim (`BookTitles` / catalog `jewishOrder`), not after Ezekiel. A Home dump that lists Daniel in Nevi'im is a contract break.
- Home spinner is `Loading Tanakh catalog…`. Treat `Could not load catalog` as a hard fail.
- `pack_torah_samples.json` may omit non-Torah books. Full 39-book order is in `app/src/main/assets/data/catalog.json`.
