# verify-tanakh evidence

Proof artifacts for `.cursor/skills/verify-tanakh`. Cleanup **must not** delete run directories.

Layout:

```
artifacts/verify-tanakh/<VERIFY_TANAKH_RUN_ID>/
  doctor.txt
  session.log
  drive-pack-sanity.log
  test-results/
  drive-apk-debug.log
  apk-debug-entries.txt
  internet-check.txt
  emulator-smoke/          # only when a device was driven
```

`.state/` is scratch for the current machine (adb serial, install flag). It is not proof.
