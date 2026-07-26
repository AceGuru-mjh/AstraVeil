# AstraVeil v0.1.0-alpha — Release Notes

## Overview

First public Alpha of AstraVeil — the Android Root Capability
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

## Known limitations

- Provider execute() is placeholder (real `su -c` wiring in 0.2)
- protobuf not yet linked into CMake build (text frame IPC)
- AstraRoot backend is a stub (Phase 7)
- No module store / OTA updates

## Minimum requirements

- Android 10 (SDK 29) or later
- ARM64 device
- SELinux enforcing (permissive supported with warnings)
