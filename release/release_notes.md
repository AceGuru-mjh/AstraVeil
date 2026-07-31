# AstraVeil v1.1.1 — Release Notes

## Overview

Root access request + notification system + permission handling.

## What's new in v1.1.1

- **Root Access Manager** — AstraVeil can now actively request root from
  the active backend via `su -c id`. This triggers Magisk/KernelSU's native
  superuser dialog. Returns GRANTED/DENIED/NO_BACKEND/ERROR. On grant,
  auto-refreshes capability matrix and app list.
- **Notification System** — 5 channels (su_requests, updates, modules,
  daemon, security) with 6 notification types. Shield notification icon.
  Initialized in Application.onCreate().
- **Permission Handling** — MainActivity now properly requests
  POST_NOTIFICATIONS (API 33+) and READ_EXTERNAL_STORAGE (API 23-32).
  Uses RequestMultiplePermissions. .avm import uses SAF (no storage
  permission needed).

## Full feature set (from v1.0.0+)

- Standard Material 3 UI (AstraCard + NavigationBar)
- Superuser Terminal (ROOT/SHELL dual-mode)
- Real Superuser management (Magisk DB via magisk --sqlite)
- Root access request (triggers native Magisk dialog)
- Notification system (5 channels, 6 types)
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
