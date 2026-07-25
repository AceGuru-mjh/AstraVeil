# AstraVeil Roadmap — v0.1 → v1.0

## Milestone 0 — Engineering foundation ✅ (this release, v0.1.0)

- [x] Multi-module Gradle project (Kotlin DSL + version catalog)
- [x] Kotlin + Compose + C++ Native + Rust module scaffolding
- [x] CMake + JNI basics
- [x] Protobuf schema (`proto/astra.proto`)
- [x] App boots and displays: AstraVeil · Core 0.1.0 · Daemon: Offline · Provider: None

## Milestone 1 — Core architecture layer ✅ (this release)

- [x] `CapabilityEngine` (Android version, ABI, kernel, SELinux, root, mount,
  namespace, overlayfs, hook)
- [x] `PermissionEngine` (NONE/SHELL/ROOT/KERNEL tiers + dangerous-approval gate)
- [x] `EventBus` (SharedFlow, open `AstraEvent` interface)
- [x] `ConfigManager`, `AstraLogger`, `SecurityManager`
- [x] UI shows device capability live

## Milestone 2 — Daemon core (next)

- [ ] `astrad` builds & runs on-device as a system service
- [ ] Unix Domain Socket server with length-prefixed framing
- [ ] Protobuf request/response (replace Phase-0 JSON payloads)
- [ ] App ↔ daemon connection: tap Refresh → daemon → device info
- [ ] Daemon lifecycle: start on boot, restart on crash

## Milestone 3 — Root Provider system (partially done)

- [x] `RootProvider` interface
- [x] `ProviderRegistry` (singleton, ordered precedence)
- [x] Magisk / KernelSU / APatch detection probes
- [x] AstraRoot stub
- [ ] `execute()` end-to-end through the active provider
- [ ] `mount()` plumbing for each backend
- [ ] UI: "Root Backend: Magisk, Version: x.y"

## Milestone 4 — Module system v1

- [x] `.avm` format spec (ZIP: module.json + runtime/ + assets/ + permission.json)
- [x] `ModuleManager` install/uninstall/enable/disable/start/stop
- [x] `ModuleValidator` (manifest + package)
- [x] `ModuleSandbox` profile computation
- [ ] `ModuleRuntime.load()` — dlopen the runtime .so and call the entry symbol
- [ ] UI: module list with running state + per-module permission chips
- [ ] `.avm` install via file open-with

## Milestone 5 — Security sandbox

- [x] Policy engine (Rust `PolicyEngine`: Allow / Deny / RequireApproval)
- [x] Sandbox profile model
- [ ] Enforce sandbox via `unshare(CLONE_NEWNS)` + bind mounts in the daemon
- [ ] seccomp/landlock filter installation per module process
- [ ] Runtime violation detection → `SecurityViolationEvent`

## Milestone 6 — Advanced compatibility

- [ ] `compatibility/devices/` database (Pixel, Xiaomi, Samsung, OPPO, vivo)
- [ ] Per-device capability overrides (e.g. Xiaomi overlayfs=false)
- [ ] Kernel-version-aware feature gating
- [ ] Fallback provider selection based on capability matrix

## Milestone 7 — AstraRoot (the future)

- [ ] Boot backend (init-time patching, not Magisk-style)
- [ ] Astra Init → Astra Daemon → Astra Permission chain
- [ ] `AstraRootProvider` implementation (no longer a stub)
- [ ] Root Capability + Permission Policy + Module Sandbox at the kernel boundary
- [ ] su binary that consults the policy engine before granting

## Milestone 8 — AstraKernel (long-term)

- [ ] Kernel Enhancement Layer (not a KernelSU clone)
- [ ] High-performance hook interface
- [ ] In-kernel security policy enforcement
- [ ] eBPF-based module observation

## Release cadence

| Version | Milestones           | Theme                              |
|---------|----------------------|------------------------------------|
| 0.1.0   | 0, 1                 | Foundation + capability probing    |
| 0.2.0   | 2                    | Live daemon + IPC                  |
| 0.3.0   | 3                    | Real provider execution            |
| 0.4.0   | 4                    | Module runtime loadable            |
| 0.5.0   | 5                    | Sandboxed modules                  |
| 0.6.0   | 6                    | Device compatibility matrix        |
| 0.7–0.9 | 7                    | AstraRoot alpha → beta             |
| 1.0.0   | 7, 8 (preview)       | AstraRoot stable + AstraKernel preview |
