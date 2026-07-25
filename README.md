<div align="center">

# AstraVeil

**Android Root Abstraction Platform**

A unified, capability-driven control plane that sits above Magisk, KernelSU,
APatch and — eventually — its own native `AstraRoot` backend.

`v0.1.0` · Phase 0 (Foundation)

</div>

---

## What is AstraVeil?

AstraVeil is **not** a Magisk fork. It is a *root abstraction platform*: a single
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
| IPC                | libsu / sockets     | Protobuf over Unix Domain Socket   |
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
├── daemon/      # `astrad` — standalone C++ system service (Unix socket + protobuf)
├── proto/       # IPC schema
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
| IPC          | Protobuf over Unix Domain Socket       |

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

Proprietary — AstraVeil Project. See `LICENSE` (to be added).
