# AstraVeil v1.6.0 — Release Notes

## Overview

Alpha security closeout: the last open **P0** item (P0-4 interim native-module
isolation) and audit **P1-12** (interactive privileged-session security) are
both closed. The **AstraHub** module repository ships as the discovery layer
for the module ecosystem. **LICENSE** and **version single-source-of-truth**
round out the release-gate compliance items.

With v1.6.0, all 6 P0 audit findings are closed and the project meets the
Alpha minimum bar.

## What's new in v1.6.0

### P1-12 — TrustedInteractiveSession (security)
Terminal and Root Test no longer bypass the structured security model. All
interactive privileged commands now flow through a single gated path:
`approve() → execute() → audit → close`.

- `CommandAuditLogger` (core/execution): append-only JSONL audit trail —
  every executed command is recorded with sessionId / source / backend /
  command / exitCode / timedOut / outputPreview. User-exportable.
- `TrustedInteractiveSession` + `InteractiveSessionFactory` (app/execution):
  the ONLY legitimate path for raw privileged command execution. Refuses to
  run unless explicitly approved; every command audited; bounded session
  lifecycle. Module execution (capability broker → Rust policy → daemon) is
  untouched and stays separate.
- `TerminalViewModel`: ROOT/ADB modes now go through `session.execute()`
  (gated + audited). SHELL mode (app-UID) stays local and non-privileged —
  it needs no privileged approval.
- `TerminalApprovalDialog`: strong acknowledgment gate (checkbox + "I
  understand") shown before the first privileged command runs.
- `StatusViewModel.testRootCapability()`: the 6 diagnostic probes now run
  through a `ROOT_TEST` session — opened, approved (the Test Root tap is
  the explicit gesture), and closed in `finally`. Every probe is audited.
- `ProviderRegistry.activeProvider()` returns the `RootProvider` instance.
  `ProviderExecResult.timedOut` added (default false, additive).

### AstraHub — module repository (ecosystem)
Curated module index with transport integrity (SHA-256 verified against the
index on download) and a clean hand-off to the existing installer which
enforces TrustGate. **Discovery-only** — does not duplicate signature or
capability enforcement.

- New `astrahub/modules/index.json` schema: `schemaVersion`,
  `requiredCapabilities` / `optionalCapabilities`, `signatureFingerprint`,
  `sha256`, `downloadUrl`, `sizeBytes`, `minAstraVeilVersion`.
- `AstraHubClient`: `fetchIndex()` / `search()` / `downloadVerified()` —
  SHA-256 re-checked against the index; mismatches are deleted and rejected.
- `AstraHubViewModel` + `AstraHubScreen` (Material 3): trust-level badges,
  capability chips, install → hand-off dialog. Reachable via
  `navController.navigate("astrahub")`.
- `docs/ASTRAHUB_SUBMISSION.md`: module submission guide.

### P0-4 — interim native load policy (security)
Until the isolated ModuleRunner (daemon fork + dlopen, Phase 1) lands,
third-party native modules are refused in-process. This is the honest
interim: we do NOT pretend to sandbox; we refuse to load untrusted native
code into the UI process.

- `NativeModuleLoadPolicy`: ALLOW vs REQUIRE_ISOLATION. Built-in or
  OFFICIAL-signed native → ALLOW in-process; third-party / developer /
  unsigned native → REQUIRE_ISOLATION (refused with a logged reason).
- `ModuleRecord.trustLevel` + `AstraModule.trustLevelName`: trust level
  flows from install → persistence → runtime.
- `ModuleManager.install()` computes a structured `SignatureVerification`
  (Ed25519 + trust chain via `ModuleSignatureVerifier`) and resolves the
  effective `TrustLevel` (stricter-wins on disagreement with the legacy
  hash check).
- `ModuleRuntime.load()` gates `System.load()` behind the policy.
- `SecurityManager.pinnedPublicKeyB64` exposed (was private) so the
  installer can pass it to the structured verifier.

### Compliance
- `LICENSE`: elaborated proprietary license (Grant / Restrictions / Module
  SDK carve-out / Disclaimer / Termination). Third-party modules developed
  against the AstraVeil SDK are explicitly NOT governed by the app license.
- Version single-source-of-truth: `:core` BuildConfig now derives
  `ASTRAVEIL_VERSION` / `ASTRAVEIL_VERSION_CODE` from `gradle.properties`;
  `Version.VERSION` / `Version.CODE` read from BuildConfig instead of being
  hardcoded (the previous `"0.1.0-alpha"` constant had drifted out of sync).
  README `v3.0` → `v1.5.0` → now `v1.6.0`.

## Audit status

| Item | Status |
|---|---|
| P0-1 IPC auth | ✅ PR-A |
| P0-2 Rust fail-closed | ✅ PR-A |
| P0-3 Zip Slip | ✅ PR-B |
| P0-4 native isolation | ✅ interim (v1.6.0) |
| P0-5 TOCTOU | ✅ PR-C |
| P0-6 keys in repo | ✅ CI scan |
| P1-12 interactive session | ✅ v1.6.0 |
| LICENSE | ✅ v1.6.0 |
| Version unity | ✅ v1.6.0 |

**6/6 P0 + key P1 items closed.**

## Known limitations (Phase 1)
- Full ModuleRunner process isolation (fork + seccomp + landlock + dlopen)
  is not yet implemented. Third-party native modules are refused in-process
  rather than sandboxed. `NativeModuleLoadPolicy.REQUIRE_ISOLATION` will
  become a daemon hand-off in Phase 1.4.
- The pinned Ed25519 release key in `SecurityManager` is a development key.
  Rotate before any production signed release and purge from git history if
  it was ever used to sign a public build.

## Verification
- CI: `:core:testDebugUnitTest`, `:app:assembleDebug`, lintDebug, daemon
  ctest, rust tests, rust-fuzz, cpp-fuzz — all green on PR #81.
- Manual (pre-merge): Terminal approval gate appears on first ROOT command;
  audit log written to `files/command_audit.jsonl`; AstraHub loads index,
  search filters, download verifies SHA-256, hand-off dialog appears.
