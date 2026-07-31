# AstraVeil v1.1.0 — Release Notes

## Overview

Major feature release: Superuser Terminal.

## What's new in v1.1.0

- **Superuser Terminal** — A built-in terminal with dual-mode execution:
  - **ROOT mode**: commands run via `su -c` through the active root provider
  - **SHELL mode**: commands run in a local `sh -c` subprocess (no root needed)
  - Command history (up/down navigation)
  - Quick command chips: id, getenforce, uname -a, magisk -v, whoami, df -h, getprop
  - Color-coded output: green commands, gray output, red errors, muted info
  - Auto-scroll to newest line
  - 2000-line output buffer
  - Built-in `clear` and `exit` commands
  - Graceful fallback when no root backend detected

## Full feature set (from v1.0.0+)

- Standard Material 3 UI (AstraCard + NavigationBar)
- Real Superuser management (Magisk DB via `magisk --sqlite`)
- Multi-probe root test (6 probes)
- Real app list from PackageManager
- Module Trust Pipeline (SHA-256 + manifest + risk + signature)
- AVM Module Runtime (install/uninstall/start/stop)
- AstraDaemon (C++20 Unix socket IPC)
- Module Runner (fork + sandbox + dlopen)
- Rust Security (PolicyEngine + SandboxPolicy + SHA-256)
- Provider Runtime (Magisk/KernelSU/APatch detection)
- DaemonManager + Magisk Module deployment package
- 126 Unit Tests + Instrumented Test scaffold
- Chinese localization (i18n)
- Dynamic diagnostics recommendations

## Minimum requirements

- Android 8.0 (API 26) or later
- ARM64 device
- Magisk (for su policy management), KernelSU, or APatch for root features

## Install

1. Install the APK: `adb install app-release.apk`
2. For daemon support: flash the Magisk module zip from releases
