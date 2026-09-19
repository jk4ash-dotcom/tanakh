#!/usr/bin/env bash
# Drive one verification path. Prefer existing gradle tasks; do not reimplement them.
#
#   drive.sh pack-sanity      # JVM: PackSanity + AssetPath + DSS unit tests (no emulator)
#   drive.sh apk-debug        # assembleDebug + verifyDebugApkAssets + INTERNET check
#   drive.sh apk-release      # assembleRelease + verifyReleaseApkAssets + INTERNET check
#   drive.sh emulator-smoke   # Compose user path on a device we launched
#   drive.sh all-jvm          # pack-sanity then apk-debug (release is slower; run apk-release explicitly)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

TARGET="${1:-pack-sanity}"

ROOT="$(verify_tanakh_find_root)"
verify_tanakh_apply_env "$ROOT"
EVIDENCE="$(verify_tanakh_evidence_dir "$ROOT")"
STATE="$(verify_tanakh_state_dir "$ROOT")"

gradle_ok() {
  if [[ -z "${ANDROID_HOME:-}" || ! -d "${ANDROID_HOME:-}" ]]; then
    echo "drive: ANDROID_HOME required for gradle. Run helpers/bootstrap-android-sdk.sh or export ANDROID_HOME." >&2
    echo "This is not a substitute proof. Do not invent passing test results." >&2
    return 1
  fi
  return 0
}

copy_test_results() {
  local dest="$EVIDENCE/test-results"
  mkdir -p "$dest"
  if [[ -d "$ROOT/app/build/test-results/testDebugUnitTest" ]]; then
    cp -a "$ROOT/app/build/test-results/testDebugUnitTest/." "$dest/" || true
  fi
}

check_internet_in_file() {
  local label="$1" file="$2"
  if [[ ! -f "$file" ]]; then
    echo "drive: missing $label at $file" >&2
    return 1
  fi
  if grep -q 'android.permission.INTERNET' "$file"; then
    echo "drive: $label declares android.permission.INTERNET" >&2
    grep -n 'INTERNET' "$file" | tee -a "$EVIDENCE/internet-check.txt" >&2
    return 1
  fi
  echo "PASS no INTERNET in $label ($file)" | tee -a "$EVIDENCE/internet-check.txt"
}

check_apk_assets_and_internet() {
  local flavor="$1" # debug|release
  local apk="$ROOT/app/build/outputs/apk/$flavor/app-$flavor.apk"
  if [[ ! -f "$apk" ]]; then
    echo "drive: missing APK $apk" >&2
    return 1
  fi
  unzip -Z1 "$apk" > "$EVIDENCE/apk-$flavor-entries.txt"
  python3 - "$EVIDENCE/apk-$flavor-entries.txt" "$flavor" <<'PY'
import sys
names = set(p.strip() for p in open(sys.argv[1], encoding="utf-8") if p.strip())
flavor = sys.argv[2]
def has(*cands):
    return any(c in names for c in cands)
ok = True
for req, cands in [
    ("catalog", ["assets/data/catalog.json"]),
    ("glosses", ["assets/data/glosses.json.gz", "assets/data/glosses.json"]),
    ("Gen", ["assets/data/books/Gen.json.gz", "assets/data/books/Gen.json"]),
]:
    if not has(*cands):
        print(f"FAIL APK {flavor} missing {req}: tried {cands}")
        ok = False
    else:
        print(f"PASS APK {flavor} has {req}")
sys.exit(0 if ok else 1)
PY
  # Merged manifest is the readable INTERNET proof; binary APK manifest is not greppable as XML.
  local merged="" process=""
  if [[ "$flavor" == "debug" ]]; then
    process="processDebugMainManifest"
  else
    process="processReleaseMainManifest"
  fi
  for cand in \
    "$ROOT/app/build/intermediates/merged_manifest/$flavor/$process/AndroidManifest.xml" \
    "$ROOT/app/build/intermediates/merged_manifests/$flavor/AndroidManifest.xml"
  do
    if [[ -f "$cand" ]]; then merged="$cand"; break; fi
  done
  if [[ -n "$merged" ]]; then
    check_internet_in_file "merged-manifest-$flavor" "$merged"
  else
    echo "WARN no merged manifest for $flavor — used APK entry list only" | tee -a "$EVIDENCE/internet-check.txt"
  fi
  # aapt dump permissions when present (same check Argus reports used).
  local aapt=""
  if command -v aapt >/dev/null 2>&1; then
    aapt="$(command -v aapt)"
  elif [[ -n "${ANDROID_HOME:-}" ]]; then
    aapt="$(ls -1 "$ANDROID_HOME"/build-tools/*/aapt 2>/dev/null | tail -n 1 || true)"
  fi
  if [[ -n "$aapt" ]]; then
    "$aapt" dump permissions "$apk" | tee "$EVIDENCE/aapt-permissions-$flavor.txt"
    if grep -q 'android.permission.INTERNET' "$EVIDENCE/aapt-permissions-$flavor.txt"; then
      echo "drive: aapt dump permissions shows INTERNET" >&2
      return 1
    fi
    echo "PASS aapt dump permissions has no INTERNET ($flavor)" | tee -a "$EVIDENCE/internet-check.txt"
  fi
}

drive_pack_sanity() {
  gradle_ok
  verify_tanakh_log "$EVIDENCE" "drive pack-sanity (testDebugUnitTest: PackSanityTest, AssetPathResolutionTest, DssVariantRepositoryTest)"
  (
    cd "$ROOT"
    ./gradlew :app:testDebugUnitTest \
      --tests com.tanakhpoc.learner.PackSanityTest \
      --tests com.tanakhpoc.learner.AssetPathResolutionTest \
      --tests com.tanakhpoc.learner.DssVariantRepositoryTest \
      --no-daemon
  ) | tee "$EVIDENCE/drive-pack-sanity.log"
  copy_test_results
  if grep -q 'BUILD SUCCESSFUL' "$EVIDENCE/drive-pack-sanity.log"; then
    verify_tanakh_log "$EVIDENCE" "drive pack-sanity PASSED"
    echo "PROOF (jvm): unit tests passed. This proves pack/gloss/LTR/YHWH/DSS invariants, not Compose navigation."
  else
    echo "drive pack-sanity: gradle did not report BUILD SUCCESSFUL" >&2
    return 1
  fi
}

drive_apk() {
  local flavor="$1"
  gradle_ok
  local task
  if [[ "$flavor" == "debug" ]]; then
    task=":app:verifyDebugApkAssets"
  else
    task=":app:verifyReleaseApkAssets"
  fi
  verify_tanakh_log "$EVIDENCE" "drive apk-$flavor ($task)"
  (
    cd "$ROOT"
    ./gradlew "$task" --no-daemon
  ) | tee "$EVIDENCE/drive-apk-$flavor.log"
  if ! grep -q 'BUILD SUCCESSFUL' "$EVIDENCE/drive-apk-$flavor.log"; then
    echo "drive apk-$flavor: gradle failed" >&2
    return 1
  fi
  check_apk_assets_and_internet "$flavor"
  verify_tanakh_log "$EVIDENCE" "drive apk-$flavor PASSED"
  echo "PROOF (apk-$flavor): packaged catalog/glosses/Gen + no INTERNET in merged manifest/aapt."
}

drive_emulator_smoke() {
  if ! command -v adb >/dev/null 2>&1; then
    echo "drive emulator-smoke: adb missing. This path is deferred. Use pack-sanity on JVM hosts." >&2
    return 2
  fi
  local serial=""
  if [[ -f "$STATE/adb-serial" ]]; then
    serial="$(cat "$STATE/adb-serial")"
  fi
  serial="${ANDROID_SERIAL:-$serial}"
  if [[ -z "$serial" ]]; then
    echo "drive emulator-smoke: no serial. Run launch.sh --mode emulator first." >&2
    return 1
  fi
  export ANDROID_SERIAL="$serial"
  verify_tanakh_log "$EVIDENCE" "drive emulator-smoke serial=$serial"
  python3 "$SCRIPT_DIR/uiautomator_drive.py" \
    --serial "$serial" \
    --evidence "$EVIDENCE" \
    --recipe verse-gloss
}

case "$TARGET" in
  pack-sanity) drive_pack_sanity ;;
  apk-debug) drive_apk debug ;;
  apk-release) drive_apk release ;;
  emulator-smoke) drive_emulator_smoke ;;
  all-jvm)
    drive_pack_sanity
    drive_apk debug
    ;;
  *)
    echo "usage: drive.sh pack-sanity|apk-debug|apk-release|emulator-smoke|all-jvm" >&2
    exit 2
    ;;
esac
