# AstraVeil v1.5.0 — Release Notes

## Overview

Security engineering completion: trust gate wired, update verifier, signature infrastructure, threat model, fuzzing, cross-backend executor.

## What's new in v1.5.0

### PR-C Completion: Module Registry + Trust Gate (wired)
- ModuleManager now loads from `.registry.json` on startup (survives restart)
- TrustGate.requireInstallable() enforced inside install() — cannot be bypassed
- verifySignature() reads ASTRAVEIL.SIG from .avm archive
- registry.save() called on install/uninstall/state-transition

### PR-D Completion: Update Chain Security
- UpdateVerifier: SHA-256 checksum + signing certificate matching
- ModuleSignatureVerifier: Ed25519 signature + manifest hash + trust levels
- DeveloperKeyStore: user-managed trust store for developer keys
- AvMSigner: CLI tool for keygen + signing .avm packages

### Security Engineering
- docs/THREAT_MODEL.md: STRIDE analysis, attack trees, residual risks
- docs/MODULE_DEVELOPER_GUIDE.md: full .avm developer documentation
- docs/SECURITY.md: vulnerability reporting policy

### Cross-Backend + Compatibility
- ShellExecutor: robust command execution with timeout + async I/O
- CapabilityCompatibilityChecker: install-before-you-try capability check

### Fuzzing
- daemon/fuzz/fuzz_frame_codec.cpp: libFuzzer IPC frame harness
- .github/workflows/fuzz.yml: CI fuzz workflow (C++ + Rust)

## Security audit status: ALL 6 PRs (A-F) fully landed
