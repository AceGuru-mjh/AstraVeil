# AstraVeil v1.0.0 — Release Notes

## Overview

First stable release of AstraVeil — the Android Root Capability
Operating Layer. Not a root tool; a capability abstraction platform
above Magisk / KernelSU / APatch / (future) AstraRoot.

## What's included

- **Liquid Glass Design System** — Multi-layer optical rendering with specular highlights, press compression, and spring bounce
- **AstraUI** — Kotlin + Compose dashboard with Glass Design System + Chinese localization (i18n)
- **AstraCore** — Capability Engine, Permission Engine (persisted), EventBus, ConfigManager (persisted), SecurityManager (Ed25519 + SHA-256)
- **Module Trust Pipeline** — SHA-256 fingerprint + manifest pre-parse + risk analysis + signature status before install
- **AVM Module Runtime** — .avm format with install/uninstall/start/stop, ModuleValidator, sandbox lifecycle, ModuleRuntime.load() via System.load + JNI_OnLoad + dlopen/dlsym entry invocation
- **AstraDaemon** — C++20 system service with Unix socket IPC (JSON frames), capability/provider/module/security services
- **Module Runner** — fork + sandbox (namespace + seccomp 40+ syscalls + Landlock) + dlopen + waitpid
- **Rust Security** — PolicyEngine (Allow/Deny/RequireApproval) + SandboxPolicy + SHA-256 attestation + 42 unit tests
- **Provider Runtime** — Magisk / KernelSU / APatch detection + intelligent provider routing
- **DaemonManager** — App-side daemon connection with retry + ping verification
- **Magisk Module Package** — service.sh + init.rc for deploying astrad at boot
- **126 Unit Tests** — core/providers/modules Kotlin tests + Rust #[cfg(test)] modules
- **CI/CD Pipelines** — Android APK, Native daemon, Rust tests, Release automation

## Known limitations

- Provider execute() is interface-only (real `su -c` wiring in v1.1)
- AstraRoot backend is a stub (Phase 7)
- No module store / OTA updates
- Daemon requires manual deployment via Magisk module

## Minimum requirements

- Android 8.0 (API 26) or later
- ARM64 device
- Java 17 or later for building

## Install

1. Install the debug APK: `adb install app-debug.apk`
2. For daemon support: flash the Magisk module zip from releases
