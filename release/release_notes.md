# AstraVeil v1.0.0 — Release Notes

## Overview

First stable release of AstraVeil — the Android Root Capability
Operating Layer. Not a root tool; a capability abstraction platform
above Magisk / KernelSU / APatch / (future) AstraRoot.

## What's included

- **AstraUI** — Kotlin + Compose dashboard with Glass Design System
- **AstraCore** — Capability Engine, Permission Engine, EventBus, Device Profile
- **AstraDaemon** — C++20 system service with Unix socket IPC + protobuf
- **Rust Security** — PolicyEngine (Allow/Deny/RequireApproval) + Sandbox validator
- **Provider Runtime** — Magisk / KernelSU / APatch detection + RootCommandExecutor
- **AVM Module Runtime** — .avm format, ModuleValidator, sandbox lifecycle
- **Native Sandbox** — unshare + seccomp + landlock isolation
- **Example Module** — hello-world.avm demonstrating the SDK surface
- **Gradle Wrapper** — Gradle 8.10.2 for consistent builds
- **CI/CD Pipelines** — Android APK, Native daemon, and Rust test workflows

## Known limitations

- Provider execute() is placeholder (real `su -c` wiring in 1.1)
- protobuf not yet linked into CMake build (text frame IPC)
- AstraRoot backend is a stub (Phase 7)
- No module store / OTA updates

## Minimum requirements

- Android 10 (SDK 29) or later
- ARM64 device
- SELinux enforcing (permissive supported with warnings)
- Java 17 or later

## Next versions

- **v1.0.1** — Performance optimizations and bug fixes
- **v1.0.2** — Additional device compatibility improvements
