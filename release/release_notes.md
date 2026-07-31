# AstraVeil v1.4.0 — Release Notes

## Overview

Security hardening complete: module registry persistence, trust gate, data provenance, CI verification.

## What's new in v1.4.0

### PR-C: Module Registry + Trust Gate
- **ModuleRegistry**: persists module list to `.registry.json` (survives app restart)
- **TrustGate**: mandatory trust verification — `requireInstallable()` throws if hash missing, manifest invalid, or signature unverified

### PR-E: Data Provenance
- **DataProvenance enum**: PROBED/DETECTED/ADVERTISED/INFERRED/UNAVAILABLE
- **ProvenancedValue<T>**: every data point carries its source
- Removed hardcoded `latencyMs=8`, `pid=0`, `daemonVersion="0.1.0"`, `daemonOnline=true`

### PR-F: CI Verification
- **Daemon tests**: peer UID whitelist (12 assertions), frame codec round-trip (5 cases)
- **Secret scan**: `check_no_secrets.sh` blocks keystore files and hardcoded passwords
- **Android CI**: now runs `testDebugUnitTest` + `lintDebug`
- **Native CI**: now runs `ctest`
- **Keystore untracked**: removed from git

## Security audit PRs A-F summary
- PR-A: IPC auth (0660 + SO_PEERCRED) + Rust fail-closed
- PR-B: Zip Slip + zip bomb + ID regex
- PR-C: Registry persistence + trust gate
- PR-D: REQUEST_INSTALL_PACKAGES + FileProvider + SHA-256 checksum
- PR-E: Data provenance (remove hardcoded values)
- PR-F: CI verification (tests + lint + ctest + secret scan)
