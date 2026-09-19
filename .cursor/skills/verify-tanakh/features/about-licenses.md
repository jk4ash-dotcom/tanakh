# About and licenses

About lists sources, Jewish navigation policy, phonetic rules, gloss policy, DSS disclaimer, and license lines. Home also exposes a Notes help dialog that is not the About screen.

## Sub-features

- `about-open` opens route `about` from the Home info icon.
- `about-licenses` shows the `Licenses` heading and the OSHB / TBESH / JPS / hebrew-transliteration / Noto line.
- `about-version` shows `App {VERSION_NAME} ({GIT_SHA}) · pack {catalog.version} · com.tanakhpoc.learner`.
- `notes-help` opens the Home help dialog (`Notes help` / `Got it`).

## How to get to it (user POV)

- On Home, tap the info icon (`contentDescription` `About`).
- Press `Back` to return to Home.
- On Home, tap the help icon (`contentDescription` `Notes help`), then `Got it`.

## Driving it with verify-tanakh

Preconditions:

- Home is showing `Tanakh Learner` (catalog loaded).
- There is no JVM test for About copy. Without an emulator, report this feature **skipped** (do not cite PackSanityTest as About proof).

- **Open About.** Tap contentDescription `About`. App bar text is `About`. Body starts with `Tanakh Learner` and includes `Navigation`, `Hebrew text`, `English (verse row)`, `Phonetics (Sofer SBL-Learner)`, `Glosses`, `Display order`, `Qumran / DSS notes`, `Licenses`.
- **Licenses line.** Visible text: `OSHB (PD + CC BY 4.0) · TBESH (CC BY 4.0, STEPBible) · JPS 1917 (PD) · hebrew-transliteration (MIT) · Noto Sans Hebrew (SIL OFL 1.1).`
- **Version stamp.** Line matches `App 0.6.0-template-poc` (or current `BuildConfig.VERSION_NAME`) and `com.tanakhpoc.learner`. Pack version is `catalog.version` (shipped `0.4.2-poc` at skill authoring — assert the catalog file, not README app version).
- **Notes help.** Tap `Notes help`. Dialog title `Notes help`. Confirm button `Got it`. Text includes `Tap a phonetic chip for possible sense(s). Gloss ≠ verse translation.` and the Qumran `Q` explanation.
- **Proof.** Uiautomator dump of About showing `Licenses` and the license sentence. Dump of the help dialog showing `Got it`. Record feature id `about-licenses`.

## Gotchas

- Launcher name is `Tanakh Learner (POC)` (`strings.xml`); About headline and Home title are `Tanakh Learner`.
- App `versionName` (`0.6.0-template-poc`) is not the pack `catalog.version`. The About footer prints both.
- Jewish-order copy on About (`Daniel is in Writings when present — not Christian/filename order`) is independent of Home list order; verify both if you claim nav policy.
- Do not open About via a guessed overflow menu. The only entry is the Home info `IconButton`.
