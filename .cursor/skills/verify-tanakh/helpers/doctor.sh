#!/usr/bin/env bash
# Read-only: is this checkout / device worth driving?
# Exit 0 = JVM verification path is healthy.
# Exit 2 = emulator/device requested but missing (JVM path may still be OK).
# Exit 1 = not worth driving.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib.sh
source "$SCRIPT_DIR/lib.sh"

REQUIRE_EMULATOR=0
if [[ "${1:-}" == "--require-emulator" ]]; then
  REQUIRE_EMULATOR=1
fi

ROOT="$(verify_tanakh_find_root)"
verify_tanakh_apply_env "$ROOT"
EVIDENCE="$(verify_tanakh_evidence_dir "$ROOT")"
REPORT="$EVIDENCE/doctor.txt"
FAILS=0
WARNS=0

note() {
  printf '%s\n' "$*" | tee -a "$REPORT"
}

pass() { note "PASS  $*"; }
fail() { note "FAIL  $*"; FAILS=$((FAILS + 1)); }
warn() { note "WARN  $*"; WARNS=$((WARNS + 1)); }

: > "$REPORT"
note "verify-tanakh doctor"
note "root=$ROOT"
note "evidence=$EVIDENCE"
note "time=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
note "---"

if [[ -x "$ROOT/gradlew" ]]; then
  pass "gradlew executable"
else
  fail "gradlew missing or not executable at $ROOT/gradlew"
fi

if grep -q 'rootProject.name = "TanakhLearner"' "$ROOT/settings.gradle.kts"; then
  pass "settings.gradle.kts rootProject.name=TanakhLearner"
else
  fail "unexpected settings.gradle.kts"
fi

if grep -q 'namespace = "com.tanakhpoc.learner"' "$ROOT/app/build.gradle.kts" \
  && grep -q 'applicationId = "com.tanakhpoc.learner"' "$ROOT/app/build.gradle.kts"; then
  pass "applicationId/namespace com.tanakhpoc.learner"
else
  fail "package is not com.tanakhpoc.learner in app/build.gradle.kts"
fi

if grep -q 'versionName = "0.6.0-template-poc"' "$ROOT/app/build.gradle.kts"; then
  pass "versionName 0.6.0-template-poc (app/build.gradle.kts)"
else
  warn "versionName in app/build.gradle.kts is not 0.6.0-template-poc — record the actual value"
  grep 'versionName' "$ROOT/app/build.gradle.kts" | tee -a "$REPORT" || true
fi

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
  pass "JAVA_HOME=$JAVA_HOME ($("$JAVA_HOME/bin/java" -version 2>&1 | head -n 1))"
else
  if command -v java >/dev/null 2>&1; then
    warn "JAVA_HOME unset; using PATH java ($(java -version 2>&1 | head -n 1))"
  else
    fail "no Java runtime (set JAVA_HOME; README expects JDK 17 at /workspace/.jdk/jdk-17.0.20.1+1)"
  fi
fi

if [[ -n "${ANDROID_HOME:-}" && -d "$ANDROID_HOME" ]]; then
  pass "ANDROID_HOME=$ANDROID_HOME"
  if [[ -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" || -x "$ANDROID_HOME/platform-tools/adb" ]]; then
    pass "SDK tools present under ANDROID_HOME"
  else
    warn "ANDROID_HOME exists but cmdline-tools/platform-tools not found — gradle assemble/test may fail"
  fi
else
  warn "ANDROID_HOME unset and no /workspace/android-sdk — JVM Drive (gradle test/assemble) cannot run until helpers/bootstrap-android-sdk.sh"
fi

DATA="$ROOT/app/src/main/assets/data"
for rel in catalog.json glosses.json.gz books/Gen.json.gz dss_variants.json; do
  if [[ -f "$DATA/$rel" ]]; then
    pass "source asset $rel"
  else
    fail "missing source asset app/src/main/assets/data/$rel"
  fi
done

if [[ -f "$ROOT/app/src/test/resources/data/pack_torah_samples.json" ]]; then
  pass "unit fixture app/src/test/resources/data/pack_torah_samples.json"
else
  fail "missing PackSanityTest fixture pack_torah_samples.json"
fi

for t in PackSanityTest.kt AssetPathResolutionTest.kt DssVariantRepositoryTest.kt; do
  if [[ -f "$ROOT/app/src/test/java/com/tanakhpoc/learner/$t" ]]; then
    pass "unit test $t"
  else
    fail "missing unit test $t"
  fi
done

if grep -q 'fun verifyApkAssets' "$ROOT/app/build.gradle.kts" \
  && grep -q 'verifyDebugApkAssets' "$ROOT/app/build.gradle.kts" \
  && grep -q 'verifyReleaseApkAssets' "$ROOT/app/build.gradle.kts"; then
  pass "gradle APK asset tasks verifyDebugApkAssets / verifyReleaseApkAssets"
else
  fail "APK asset verification tasks missing from app/build.gradle.kts"
fi

MANIFEST="$ROOT/app/src/main/AndroidManifest.xml"
if [[ -f "$MANIFEST" ]]; then
  if grep -q 'android.permission.INTERNET' "$MANIFEST"; then
    fail "source AndroidManifest.xml declares android.permission.INTERNET (offline POC forbids this)"
  else
    pass "source AndroidManifest.xml has no INTERNET permission"
  fi
  if grep -q 'android.permission.ACCESS_NETWORK_STATE\|android.permission.WRITE_EXTERNAL_STORAGE\|android.permission.ACCESS_FINE_LOCATION' "$MANIFEST"; then
    fail "source AndroidManifest.xml declares storage/location/network-state permission"
  else
    pass "source AndroidManifest.xml has no storage/location/network-state permissions"
  fi
else
  fail "missing $MANIFEST"
fi

set +e
python3 - "$DATA/catalog.json" "$REPORT" <<'PY'
import json, sys
path, report = sys.argv[1], sys.argv[2]
cat = json.load(open(path, encoding="utf-8"))
def w(msg):
    print(msg)
    open(report, "a", encoding="utf-8").write(msg + "\n")
books = cat.get("books") or []
osis = [b.get("osis") for b in books]
ok = True
if cat.get("navOrder") != "Jewish Tanakh":
    w(f"FAIL  catalog.navOrder={cat.get('navOrder')!r} expected 'Jewish Tanakh'"); ok = False
else:
    w("PASS  catalog.navOrder=Jewish Tanakh")
if osis[:1] != ["Gen"]:
    w(f"FAIL  catalog first book {osis[:1]} expected ['Gen']"); ok = False
else:
    w("PASS  catalog first book Gen")
if osis[-1:] != ["2Chr"]:
    w(f"FAIL  catalog last book {osis[-1:]} expected ['2Chr']"); ok = False
else:
    w("PASS  catalog last book 2Chr")
if "Dan" not in osis:
    w("FAIL  catalog missing Dan"); ok = False
else:
    dan, ezek, esth = osis.index("Dan"), osis.index("Ezek") if "Ezek" in osis else -1, osis.index("Esth") if "Esth" in osis else -1
    if esth >= 0 and dan < esth:
        w(f"FAIL  Dan (index {dan}) is before Esth — expected Writings placement after Esther"); ok = False
    elif ezek >= 0 and dan < ezek:
        w(f"FAIL  Dan (index {dan}) is before Ezek — Christian/filename order, not Jewish Tanakh"); ok = False
    else:
        w(f"PASS  Dan is in Writings position (index {dan}, after Esth)")
totals = cat.get("totals") or {}
if totals.get("books") != 39:
    w(f"FAIL  catalog.totals.books={totals.get('books')} expected 39"); ok = False
else:
    w("PASS  catalog.totals.books=39")
w(f"INFO  catalog.version={cat.get('version')} scope={cat.get('scope')!r} verses={totals.get('verses')} glosses={totals.get('glosses')}")
sys.exit(0 if ok else 1)
PY
CATALOG_RC=$?
set -e
if [[ "$CATALOG_RC" -ne 0 ]]; then
  FAILS=$((FAILS + 1))
fi

# Optional: merged-manifest INTERNET check if a prior assemble exists.
MERGED=""
for cand in \
  "$ROOT/app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml" \
  "$ROOT/app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml" \
  "$ROOT/app/build/intermediates/merged_manifests/release/AndroidManifest.xml" \
  "$ROOT/app/build/intermediates/merged_manifests/debug/AndroidManifest.xml"
do
  if [[ -f "$cand" ]]; then MERGED="$cand"; break; fi
done
if [[ -n "$MERGED" ]]; then
  if grep -q 'android.permission.INTERNET' "$MERGED"; then
    fail "merged manifest $MERGED declares INTERNET"
  else
    pass "merged manifest has no INTERNET ($MERGED)"
  fi
else
  note "INFO  no merged manifest yet (assemble not run) — APK INTERNET check deferred to drive apk-*"
fi

if command -v adb >/dev/null 2>&1; then
  DEVICES="$(adb devices | awk 'NR>1 && $2=="device" {print $1}')"
  if [[ -n "$DEVICES" ]]; then
    pass "adb device(s): $(echo "$DEVICES" | tr '\n' ' ')"
    if adb shell pm path com.tanakhpoc.learner >/dev/null 2>&1; then
      pass "package com.tanakhpoc.learner installed on default adb device"
    else
      warn "package not installed — run launch --mode emulator"
    fi
  else
    warn "adb present but no device in 'device' state"
    if [[ "$REQUIRE_EMULATOR" -eq 1 ]]; then
      fail "--require-emulator set and no adb device"
    fi
  fi
else
  warn "adb not on PATH — Compose UI Drive is deferred (JVM unit tests still valid)"
  if [[ "$REQUIRE_EMULATOR" -eq 1 ]]; then
    fail "--require-emulator set and adb missing"
  fi
fi

note "---"
note "fails=$FAILS warns=$WARNS"
if [[ "$FAILS" -gt 0 ]]; then
  note "RESULT=UNHEALTHY"
  exit 1
fi
if [[ "$REQUIRE_EMULATOR" -eq 1 ]]; then
  note "RESULT=HEALTHY (emulator required and present)"
  exit 0
fi
note "RESULT=HEALTHY (JVM path). Emulator/device Compose Drive is a separate Launch mode."
exit 0
