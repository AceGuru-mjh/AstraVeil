# AstraVeil v1.8.0 — Release Notes

## Overview

**Signed release fix.** All previous releases (v1.4.0–v1.7.0) shipped
unsigned APKs because the signing keystore was not available in CI. This
release fixes the signing pipeline: the keystore is stored as an encrypted
GitHub secret (base64-encoded), decoded at build time, and the release
workflow verifies the APK is signed before publishing.

## What's new in v1.8.0

### Signed APK pipeline (fix)
- New keystore generated with alias `mengjinghao`, RSA 2048, valid 100 years
- Keystore stored as `ASTRAVEIL_KEYSTORE_BASE64` GitHub secret (NOT committed
  to the public repo — security best practice for a public repository)
- `release.yml` now decodes the keystore from the secret before building,
  verifies the APK is signed (filename must NOT contain `unsigned`), and
  fails the build if signing fails
- Previous v1.4.0–v1.7.0 releases had `app-release-unsigned.apk`; from
  v1.8.0 the asset is `app-release.apk` (signed)

### Signing credentials
- **Keystore alias:** `mengjinghao`
- **Certificate CN:** `CN=mengjinghao, OU=AstraVeil, O=AstraVeil, C=CN`
- **SHA-256 fingerprint:** `05:60:E4:95:27:98:2D:09:73:1C:4A:E6:12:41:55:7C:59:FE:C8:9D:61:78:C5:37:E7:AD:1E:53:51:7B:BE:C6`
- **Validity:** Aug 1 2026 → Jul 8 2126 (100 years)

### Security improvement
The keystore is no longer committed to the git repository (it was removed
in a prior commit). It is injected into CI via an encrypted GitHub secret,
so the private key is never exposed in the public repo history.

## Verification
- CI: all 5 checks green on PR (build-and-test, daemon-build-and-test,
  rust, rust-fuzz, cpp-fuzz)
- Release workflow: "Verify APK is signed" step confirms the output
  filename is `app-release.apk` (not `app-release-unsigned.apk`)
- The APK can be installed on Android 8.0+ (minSdk 26)

## Note on updates
This release uses a NEW signing certificate. It cannot be installed as an
update over any previous release that was signed with a different key
(the v1.1.1–v1.3.1 releases used an older `root8888` key; v1.4.0–v1.7.0
were unsigned). Uninstall any previous version before installing v1.8.0.
