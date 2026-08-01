# AstraVeil v2.1.0 — Release Notes

## Overview

Terminal upgraded from "command executor" to "persistent shell session".
Root Chain Self-Test added so the terminal chain can be diagnosed on-device.
P2-18 (diagnostics provenance) and P2-20 (canonical manifest) landed.

## What's new in v2.1.0

### Terminal Phase 1 — Persistent Shell Session
- **ShellSession.kt**: keeps ONE shell process alive, pipes commands to stdin.
  `cd` / `export` persist across commands, no per-command su handshake, output
  streams line-by-line. Completion detected via injected marker
  (`___ASTRA_DONE_<exit>|<cwd>___`) which yields exit code + cwd and is
  stripped from displayed output.
- **TerminalViewModel**: rewritten to use ShellSession. ROOT/ADB/SHELL modes
  start `listOf("su")` / `listOf("su","2000")` / `listOf("sh")`. Security
  model preserved: TrustedInteractiveSession approval gate + CommandAuditLogger.
  New: `cwd` StateFlow (live working directory), `interrupt()` (kill shell).
- **TerminalScreen**: added cwd status bar (teal monospace), Interrupt button
  (visible when running), Root Chain Self-Test panel.

### Milestone 0 — Root Chain Self-Test
- **RootChainSelfTest.kt**: runs the 7 links of the terminal chain on-device:
  ① backend detect → ② su binary → ③ one-shot su → ④ uid=0 → ⑤ authorized →
  ⑥ persistent shell → ⑦ marker round-trip. Each step shows real command
  output as evidence; each FAIL comes with a hint telling the user what to do.
  **Step ④ PASS (uid=0) = Milestone 0 achieved.**
- **SelfTestPanel**: expandable panel in TerminalScreen. 7 steps light up
  PENDING → RUNNING → PASS/FAIL with evidence + hints.

### P2-18 — DiagnosticEngine with provenance
- **DiagnosticEngine.kt** (app/diagnostics): produces `DiagnosticConclusion`
  list with `DataProvenance` badge + source string + `implemented` flag.
  Device facts = DETECTED (android.os.Build). Capabilities = PROBED/DETECTED
  from active provider. Subsystems honestly marked: daemon IPC, module
  isolation (Prototype), Rust policy (Inferred), SELinux (Unavailable).
  Reads daemon state directly via `AstraVeilApplication.daemonManager.state`.

### P2-20 — Canonical ModuleManifest (incremental, safe)
- **modules/model/ModuleManifest.kt**: canonical v3 manifest (id, name,
  version, apiVersion, permissions: List<ModulePermission>, required/
  optionalCapabilities, runtime, entry, minApi). `permissionNames` accessor
  for Phase-0 compat.
- **modules/model/LegacyManifestCompat.kt**: `legacyToCanonical()` converter
  + `toCanonical()` extensions for both existing manifests (Phase-0
  `modules.ModuleManifest` and v3 `modules.api.ModuleManifest`). Additive —
  existing code unchanged, new install-path code converts to canonical.

## Honest boundaries (Phase 1)
- Full-screen interactive programs (top/vi/less) and true SIGINT need a real
  PTY (Phase 2, native). This pipe-based session covers 90% of root-management
  commands (id/getprop/pm/dumpsys/magisk/ls/grep/mount).
- The self-test is the key new capability: it tells you exactly which link of
  the terminal chain breaks on YOUR device, with real evidence.

## Verification
- CI: all 5 checks green on PR #91.
- 0 broken imports; DiagnosticEngine correctly placed in :app (not :core).
