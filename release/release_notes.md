# AstraVeil v1.0.0 — Release Notes

## Overview

First stable release of AstraVeil — the Android Root Capability
Operating Layer. Not a root tool; a capability abstraction platform
above Magisk / KernelSU / APatch / (future) AstraRoot.

## Features

- **Liquid Glass Design System** — 10-layer optical renderer with edge
  refraction (12 concentric strokes), chromatic aberration (RGB split),
  specular highlight, elastic press with overshoot wobble, multi-layer
  border. Window-level FLAG_BLUR_BEHIND (API 31+) for real frosted glass.
- **Liquid Glass Navigation Bar** — Floating pill-shaped bar with
  windowInsetsPadding(navigationBars), animated icon/label colors.
- **Real Superuser Management** — Reads/writes Magisk's /data/adb/magisk.db
  via su -c sqlite3. Policy changes (Allow/Ask/Deny) take effect
  immediately. Shows recent su request logs. No-root devices show an
  informative amber card (not a red error) with supported backend list.
- **Multi-Probe Root Test** — 6 read-only probes (id, getenforce, uname,
  ls /data/adb, which su, mounts) with per-probe ✅/❌ display.
- **AstraUI** — Compose dashboard with Chinese localization (i18n).
- **AstraCore** — Capability Engine, Permission Engine (persisted),
  EventBus, ConfigManager (persisted), SecurityManager (Ed25519 + SHA-256).
- **Module Trust Pipeline** — SHA-256 fingerprint + manifest pre-parse +
  risk analysis + signature status before install.
- **AVM Module Runtime** — install/uninstall/start/stop with real
  System.load + JNI_OnLoad + dlopen/dlsym entry invocation.
- **AstraDaemon** — C++20 system service with Unix socket IPC (JSON frames).
- **Module Runner** — fork + sandbox (namespace + seccomp 40+ syscalls +
  Landlock full ABI v1) + dlopen + waitpid.
- **Rust Security** — PolicyEngine (Allow/Deny/RequireApproval) +
  SandboxPolicy + SHA-256 attestation + 42 unit tests.
- **Provider Runtime** — Magisk / KernelSU / APatch detection +
  intelligent provider routing.
- **DaemonManager** — App-side daemon connection with retry + ping.
- **Magisk Module Package** — service.sh + init.rc for deploying astrad.
- **126 Unit Tests** — core/providers/modules Kotlin tests + Rust tests.
- **CI/CD Pipelines** — Android APK, Native daemon, Rust tests, Release.

## Minimum requirements

- Android 8.0 (API 26) or later
- ARM64 device
- Magisk (for su policy management), KernelSU, or APatch for root features
- Java 17 or later for building

## Install

1. Install the APK: `adb install app-release.apk`
2. For daemon support: flash the Magisk module zip from releases
