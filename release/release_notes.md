# AstraVeil v1.3.1 — Release Notes

## Overview

Critical security fixes: IPC authentication, Rust policy fail-closed, Zip Slip protection, update chain hardening.

## What's new in v1.3.1

### P0-1: IPC Authentication (Local Privilege Escalation fix)
- Socket permissions tightened: 0666 → 0660 (owner+group only)
- SO_PEERCRED authentication on every connection (uid whitelist: root, system, shell, app UIDs)
- Read timeout (10s poll) to prevent slowloris attacks

### P0-2: Rust Policy Fail-Closed
- Weak fallback changed from Allow (0) to Deny (1)
- New `policy_is_available()` marker symbol: PolicyBridge denies all if Rust not linked
- Eliminates the local privilege escalation chain (0666 socket + fail-open policy)

### P0-3: AVM Unpack Hardening
- Zip Slip: canonical path validation on every entry
- Zip bomb: max 1024 entries, 50MB single file, 200MB total, 100:1 compression ratio
- Module ID regex validation (prevent path injection)

### PR-D: Update Chain Security
- `REQUEST_INSTALL_PACKAGES` permission declared
- `FileProvider` configured for secure APK install
- Release workflow now generates and uploads SHA-256 checksum
