# Pack load

Pack load is the offline catalog + gzip book/gloss pipeline the UI waits on. The user sees a spinner, then books; opening a book may spin again; opening a verse waits for glosses.

## Sub-features

- `pack-catalog` loads `assets/data/catalog.json` on startup (`PackRepository.load`) so Home can render.
- `pack-book` lazy-loads `assetGz` / `asset` for one OSIS book (`ensureBook`) with gz-then-plain fallback (aapt2 may drop `.gz`).
- `pack-glosses` loads `glosses.json.gz` or `glosses.json` on first verse (`ensureGlosses`); Home is not blocked on gloss parse.
- `pack-apk` packaged APK still contains catalog + glosses + `Gen` (`verifyDebugApkAssets` / `verifyReleaseApkAssets`).
- `pack-offline` source and merged manifests do not declare `INTERNET`.

## How to get to it (user POV)

- Cold start: `Loading Tanakh catalog…`, then Home. Failure: `Could not load catalog` plus the error message.
- Open a book: `Loading {BookTitles.title}…` (e.g. `Loading Genesis…`).
- Open a verse: indeterminate spinner until `ensureBook` + `ensureGlosses` succeed.
- DSS pack is optional: load failure yields empty chrome, not a catalog error (`TanakhApp`).

## Driving it with verify-tanakh

Preconditions:

- Source assets exist (doctor). Gradle Drive needs `ANDROID_HOME`.
- Do not run `tools/pipeline` unless the task is pack regeneration (out of scope for UI verify).

- **Doctor assets.** `verify-tanakh doctor` must PASS `catalog.json`, `glosses.json.gz`, `books/Gen.json.gz`, `dss_variants.json`, and catalog totals `books=39`.
- **Source + parse fallback.** `verify-tanakh drive pack-sanity` runs `AssetPathResolutionTest` (`assetCandidates_prefersGzThenPlain_dedupes`, `readAssetText_readsGzipMagic`, `sourceAssets_shipGlossesGzAndCatalog`) and DSS `sourceAsset_batch1HasExpectedShipCounts` (`status=sofer-signed-batch1`, 16 shipped notes).
- **Debug APK.** `verify-tanakh drive apk-debug` → existing task `:app:verifyDebugApkAssets` (also hooked from `:app:check`). Then INTERNET check on merged manifest / `aapt dump permissions`.
- **Release APK.** `verify-tanakh drive apk-release` → `:app:verifyReleaseApkAssets` (release uses debug keystore — README). Same INTERNET check.
- **Compose load (emulator only).** `launch --mode emulator` must not finish on `Could not load catalog`. Opening Genesis may show `Loading Genesis…` then `Genesis 1`.
- **Proof.** `doctor.txt`; `drive-pack-sanity.log` + JUnit XML; `apk-debug-entries.txt` or `apk-release-entries.txt`; `internet-check.txt`. UI load proof is the Home dump after launch, not gradle success.

## Gotchas

- aapt2 often stores `data/glosses.json` and `data/books/Gen.json` **without** `.gz`. The verify tasks already accept either name. Re-implementing a gz-only check will false-fail.
- `PackSanityTest` parses a combined Torah **sample** JSON. It does not open the shipped `Gen.json.gz`. Use `AssetPathResolutionTest` + APK tasks for ship assets.
- `noCompress += "gz"` is set; still assume aapt2 may decompress.
- Emoji2 stays on the classpath; `EmojiCompatInitializer` is removed in the manifest so release does not fetch fonts. That is not an INTERNET permission, but Argus reports still check INTERNET ABSENT — keep the permission check.
- Two gradle Daemons on one machine are fine; two UI sessions on one device are not.
