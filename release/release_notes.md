# AstraVeil v1.0.2 — Release Notes

## Overview

Patch release with real Magisk su policy management. The Superuser screen
now reads and writes Magisk's actual su database — changes take effect
immediately when any app calls `su`.

## What's new in v1.0.2

- **Real Magisk su Policy Management** — Superuser screen now reads/writes
  `/data/adb/magisk.db` via `su -c sqlite3`. Policy changes (Allow/Ask/Deny)
  take effect immediately: open Termux → type `su` → Magisk checks the
  database → uses the policy AstraVeil set.
- **Su Request Logs** — Reads Magisk's `logs` table and displays the 30
  most recent su requests with app name, uid, action (allow/deny), and
  timestamp.
- **Su Authorization Dialog** — New `SuRequestDialog` component for Phase 1
  real-time su prompts (Deny / Allow Once / Always).
- **3-Way Policy Selector** — Each policy entry shows Allow/Ask/Deny
  selector matching Magisk's policy values (0=deny, 1=ask, 2=allow).
- **No-Root Handling** — Devices without Magisk/KernelSU/APatch show a
  clear error message instead of fake switches.

## How it works

```
User opens Superuser → detect Magisk provider → read policies table
  → display real entries → user changes Allow/Ask/Deny
  → write to Magisk DB → open Termux → type su
  → Magisk checks DB → uses the policy AstraVeil set
```

## Full feature set (from v1.0.0+)

- Liquid Glass Design System + Navigation Bar
- Real Superuser (Magisk DB read/write)
- Multi-Probe Root Test (6 probes)
- Module Trust Pipeline (SHA-256 + manifest + risk + signature)
- AVM Module Runtime (install/uninstall/start/stop with System.load + dlopen)
- AstraDaemon (C++20 Unix socket IPC)
- Module Runner (fork + sandbox + dlopen)
- Rust Security (PolicyEngine + SandboxPolicy + SHA-256)
- Provider Runtime (Magisk/KernelSU/APatch detection + routing)
- DaemonManager + Magisk Module deployment package
- 126 Unit Tests + Instrumented Test scaffold
- Chinese localization (i18n)

## Minimum requirements

- Android 8.0 (API 26) or later
- ARM64 device
- Magisk (for su policy management), KernelSU, or APatch for root features

## Install

1. Install the APK: `adb install app-release.apk`
2. For daemon support: flash the Magisk module zip from releases
