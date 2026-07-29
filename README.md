<div align="center">

# AstraVeil

**Android Root Capability Operating Layer**

Not a root tool. Not a Magisk clone. AstraVeil is a capability operating
layer that abstracts root backends (Magisk / KernelSU / APatch / AstraRoot)
behind a capability matrix, a Rust security policy, and isolated module
execution.

`v3.0` · Architecture Refactor

</div>

---

## Provider Philosophy

> **AstraVeil does not create root. AstraVeil abstracts root capabilities.**

This is the single most important sentence in the project. AstraVeil is
frequently misread as "another root tool". It is not. AstraVeil never obtains
root itself — it discovers which root backend is present, probes what that
backend + the device + the kernel + SELinux + the boot layout actually allow,
and exposes the result as a **capability matrix** that modules and the UI read
instead of guessing.

The difference:

| | Root tool | AstraVeil |
|---|---|---|
| Question it answers | "is there root?" | "what can the device do?" |
| Module trust | module = root | module runs isolated, root is brokered |
| Backend coupling | hardcoded | pluggable `RootProvider` |
| Security model | root = all permissions | Kotlin intent → Rust policy → C++ execution |

## Trust Model

Trust descends every layer. Each layer does only its job and never trusts the
layer above it more than necessary.

```
User          (intent: authorise)
  ↓
AstraUI       (interaction)
  ↓
AstraCore     (judgement + coordination, Kotlin)
  ↓
Rust Policy   (Allow / Deny / RequireApproval + risk)
  ↓
AstraDaemon   (execution only, C++20)
  ↓
RootProvider  (capability provider)
  ↓
Kernel        (hardware, fully untrusted)
```

- **Kotlin AstraCore** owns user intent, permission state, config, UI.
- **Rust Security** owns the policy decision, risk score, module verification.
- **C++ AstraDaemon** owns execution, namespace, mount, process, IPC — and
  **does not trust any module**. Modules run in their own isolated process
  (`ModuleRunner`), never inside the daemon.

---

## What is AstraVeil?

AstraVeil is a *root abstraction platform*: a single
control plane that abstracts over every existing Android root backend through
one plugin interface — `RootProvider` — and adds a capability engine, a
permission broker, a sandboxed module runtime and (future) its own kernel-level
root implementation.

```
                         AstraUI  (Compose)
                            |
                    Astra Control API
                            |
                    AstraCore Engine
                            |
        ┌──────────────────┼──────────────────┐
   Permission          Capability          Module Runtime
    Engine               Engine               Engine
                            │
                      AstraDaemon  (C++)
                            │
        ┌──────────────────┼──────────────────┐
      Magisk         KernelSU/APatch         AstraRoot
                            │
                      Kernel Layer
```

### Why not Magisk's structure?

Magisk couples the su daemon, the module system and the hide mechanism into one
monolithic binary with `su = all permissions`. AstraVeil inverts that:

| Concern            | Magisk              | AstraVeil                          |
|--------------------|---------------------|------------------------------------|
| Root backend       | Hard-coded          | Pluggable `RootProvider` interface |
| Module permissions | `su` (everything)   | Declared + brokered + sandboxed    |
| Backend detection  | `if Magisk`         | Registry + capability probing      |
| IPC                | libsu / sockets     | JSON frames over Unix Domain Socket (Phase 0); Protobuf planned for Phase 1 |
| Kernel story       | Boot patches        | Dedicated `AstraKernel` layer      |

## Module layout

```
AstraVeil/
├── app/         # AstraUI — Compose control center
├── core/        # AstraCore — capability / permission / event / config / logger / security
├── providers/   # RootProvider abstraction + Magisk / KernelSU / APatch / AstraRoot
├── sdk/         # Public stable facade for third-party .avm modules
├── modules/     # Astra Module runtime (.avm install / sandbox / lifecycle)
├── native/      # C++20 JNI bridge (capability probing, su-path scan)
├── rust/        # Rust security crate (policy engine / sandbox / attestation)
├── daemon/      # `astrad` — standalone C++ system service (Unix socket + JSON frames)
├── proto/       # IPC schema (Phase 1 target — not yet wired into the build)
├── docs/        # Architecture & roadmap
└── build.gradle.kts
```

## Technology stack

| Layer        | Tech                                   |
|--------------|----------------------------------------|
| UI           | Kotlin + Jetpack Compose + Material 3  |
| Core / SDK   | Kotlin 2.0, kotlinx-serialization      |
| Native bridge| C++20 + CMake + JNI                    |
| Security     | Rust (serde, sha2)                     |
| Daemon       | C++20 standalone executable            |
| IPC          | JSON frames over Unix Domain Socket (Phase 0); Protobuf planned for Phase 1 |

## Current status — Phase 0 (v0.1.0)

This milestone establishes the foundation that the next several years will build
on. It deliberately does **not** implement root acquisition.

- ✅ Multi-module Gradle project (Kotlin DSL + version catalog)
- ✅ Core engine: `CapabilityEngine`, `PermissionEngine`, `EventBus`,
  `ConfigManager`, `AstraLogger`, `SecurityManager`
- ✅ Compose AstraUI dashboard: system status, capability panel, provider panel,
  modules panel
- ✅ `RootProvider` interface + `Magisk` / `KernelSU` / `APatch` detection +
  `AstraRoot` stub
- ✅ `.avm` module manager + validator + sandbox profile
- ✅ C++ JNI bridge (`libastra_native.so`) for procfs/sysfs capability reads
- ✅ Rust policy/sandbox/attestation crate
- ✅ `astrad` daemon skeleton (Unix socket server, capability/provider service,
  command executor)
- ✅ Protobuf IPC schema

## Build

> Requires Android Studio Ladybug+, JDK 17, Android NDK, and (for the Rust
> crate) `cargo-ndk`.

```bash
# From the AstraVeil/ root
./gradlew :app:assembleDebug

# Build the daemon (on a Linux/Android host with a C++20 toolchain)
cmake -S daemon -B daemon/build && cmake --build daemon/build

# Build the Rust crate for arm64
cd rust && cargo ndk -t arm64-v8a build --release
```

## Roadmap

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the v0.1 → v1.0 plan and
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the layered design.

## License

Proprietary — AstraVeil Project. See [LICENSE](LICENSE).
