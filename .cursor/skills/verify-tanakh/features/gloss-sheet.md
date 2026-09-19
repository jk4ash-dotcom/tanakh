# Gloss sheet

The gloss sheet is a modal bottom sheet that opens when the user taps a Hebrew or phonetic chip. It shows possible lexicon senses and must not present a Jehovah / ye.ho.vah dump as fact.

## Sub-features

- `gloss-open` shows title `Possible sense(s)` and footer `Gloss ≠ verse translation`.
- `gloss-senses` shows sanitized primary / bullet senses / optional definition and `Source: …`.
- `gloss-yhwh` on divine-name tokens shows `Divine name: יהוה / YHWH (no vocalization invented)` and hides Jehovah text.
- `gloss-none` shows `No gloss available for this token.` when the lemma does not resolve.
- `gloss-proclitic` shows a functional role and `Functional proclitic role — not a lexical gloss` for `pfx:` / `proclitic-functional` stacks.

## How to get to it (user POV)

- From a verse, tap a phonetic chip (Home Notes help also says this) or the Hebrew chip of the same token.
- Dismiss by dragging the sheet away (`onDismissRequest` clears `selected`).
- Notes help on Home (`Notes help` icon) repeats `Tap a phonetic chip for possible sense(s). Gloss ≠ verse translation.`

## Driving it with verify-tanakh

Preconditions:

- Compose: verse `Genesis 1:1` visible. JVM: `PackSanityTest` / `GlossDisplay` tests.

- **Compose open.** Tap `bĕrēʾshîth` (first Gen.1.1 token). Dump `05-gloss.xml` must contain `Possible sense(s)` and `Gloss ≠ verse translation`. Primary sense for `H7225` is beginning/first language (`gloss_resolve_knownIds`).
- **Forbidden strings.** The gloss dump lowercased must not contain `jehovah`, `ye.ho.vah`, or `yehovah`.
- **JVM policy.** `verify-tanakh drive pack-sanity` covers `glossDisplay_yhwh_hidesJehovahDump`, `glossDisplay_stripsYeHovahFromElohimDefinition`, `glossDisplay_sanitizeDefinition_globalForbidden`, `glossDisplay_h3071_jehovahNissi_doesNotLeakPrimary`, `glossDisplay_procliticCompound_functionalNotFakeTbesh`.
- **YHWH token (Compose, optional).** Open `Genesis 2:4` (Home → Genesis → Genesis 2 → `2:4`) and tap `YHWH`. Sheet must include `Divine name: יהוה / YHWH (no vocalization invented)` and must not include Jehovah dumps.
- **Proof.** UI: before/after dumps around the tap. JVM: JUnit XML for the `glossDisplay_*` methods. Fixture gloss `H3068` raw definition still contains `Jehovah` in the pack — the **displayed** object must not.

## Gotchas

- English on the verse page is JPS 1917 **verse-level**, not the gloss. Do not require chip text to match the English row (`jps_verseLevel_notWordAligned`).
- `GlossDisplay.forToken` is the UI oracle; do not assert raw `Gloss.definition` on the sheet.
- Opening Notes help is not opening the gloss sheet. The dialog title is `Notes help`; the sheet title is `Possible sense(s)`.
- DSS sheet (`DssVariantBottomSheet`) is a different modal: `Qumran reading` / `Does not replace the Masoretic/OSHB text`. Do not treat it as a gloss proof.
