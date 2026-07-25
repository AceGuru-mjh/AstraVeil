# AstraVeil Architecture

## Design philosophy

AstraVeil is built around three convictions:

1. **Root is a capability, not an identity.** "Is this process root?" is the
   wrong question. The right question is "what set of capabilities does this
   module hold, and are they sufficient for the operation it wants to perform?"
2. **Backends are plugins.** Magisk, KernelSU, APatch and AstraRoot are all just
   implementations of one `RootProvider` interface. No call site ever branches
   on `if (Magisk)`.
3. **Modules are confined, not privileged.** A Magisk module runs as uid 0 and
   can do anything root can. An AstraVeil module runs inside a sandbox profile
   with a declared permission ceiling.

## Layered model

```
┌──────────────────────────────────────────────────────────────┐
│  AstraUI  —  Compose control center (app/)                    │
│      Dashboard · Capability · Provider · Modules              │
└───────────────────────────────┬──────────────────────────────┘
                                │  AstraCore facade (in-process)
┌───────────────────────────────▼──────────────────────────────┐
│  AstraCore Engine  —  core/                                   │
│  ┌──────────────┐ ┌──────────────┐ ┌───────────────────────┐  │
│  │ Capability   │ │ Permission   │ │ Event Bus (SharedFlow)│  │
│  │ Engine       │ │ Engine       │ │                       │  │
│  └──────────────┘ └──────────────┘ └───────────────────────┘  │
│  ┌──────────────┐ ┌──────────────┐ ┌───────────────────────┐  │
│  │ Config Mgr   │ │ Security Mgr │ │ Logger (ring buffer)  │  │
│  └──────────────┘ └──────────────┘ └───────────────────────┘  │
└───────────────────────────────┬──────────────────────────────┘
                                │  (Phase 1: protobuf IPC)
┌───────────────────────────────▼──────────────────────────────┐
│  AstraDaemon  —  daemon/  (standalone C++20 service `astrad`) │
│  Unix Domain Socket · CapabilityService · ProviderService     │
│  CommandExecutor · Sandbox                                     │
└───────────────────────────────┬──────────────────────────────┘
                                │  RootProvider interface
        ┌───────────────────────┼───────────────────────┐
┌───────▼────────┐  ┌───────────▼──────────┐  ┌─────────▼──────────┐
│ MagiskProvider │  │ KernelSU/APatch      │  │ AstraRootProvider  │
│ providers/     │  │ providers/           │  │ providers/         │
│ magisk/        │  │ kernelsu/ apatch/    │  │ astraroot/         │
└────────────────┘  └──────────────────────┘  └─────────┬──────────┘
                                                        │ (future)
                                                ┌───────▼────────┐
                                                │  AstraKernel    │
                                                │  (kernel layer) │
                                                └────────────────┘
```

## Module responsibilities

### `:core` — AstraCore Engine
The brain. Pure Kotlin, no root. Detects device capabilities, brokers
permissions, broadcasts events, persists config. Every other module depends on
this one.

**CapabilityEngine** probes the device *without* requiring root: reads
`/proc/version`, `/sys/fs/selinux/enforce`, `/proc/filesystems`,
`/proc/self/ns/*`, scans for `su` binaries. The resulting `CapabilityInfo`
drives every later decision (which provider to try, which features to expose).

**PermissionEngine** is the broker. Permissions are free-form strings, but a
small set ("mount", "su", "kernel_hook", "namespace") are *dangerous* and
require explicit user approval. Every grant/revoke emits an event.

**EventBus** is a `MutableSharedFlow<AstraEvent>` (replay 0, buffer 64,
drop-oldest). `AstraEvent` is a plain open interface so `:providers` and
`:modules` can declare their own subtypes without cyclic dependencies.

### `:providers` — Root abstraction layer
The key pillar. One interface:

```kotlin
interface RootProvider {
    val id: String
    val displayName: String
    suspend fun available(): Boolean
    suspend fun detect(): RootInfo
    suspend fun execute(command: String): ProviderExecResult
    suspend fun info(): RootInfo
    suspend fun mount(source: String, target: String, options: String): Boolean
}
```

`ProviderRegistry` is a singleton holding the ordered list of providers
(AstraRoot → Magisk → KernelSU → APatch). `detectActive()` returns the first
available. **Adding a new backend is a one-file change**: implement
`RootProvider`, add it to the list.

### `:app` — AstraUI
A Compose control center. Not a root manager — a *system control center*. Four
destinations: Dashboard, Capability, Provider, Modules. Dark-first, violet/teal
accent, sticky bottom navigation.

### `:native` — JNI bridge
`libastra_native.so`. C++20. Exposes procfs/sysfs reads to Kotlin so the
capability engine can use native parsing where Java is awkward. Also scans su
binary paths.

### `:rust` — Security crate
`astra_rust` staticlib. `PolicyEngine` (decision: Allow/Deny/RequireApproval
with a dangerous-permission denylist), `Sandbox` (profile validation), and
`attestation` (SHA-256 + module attestation tokens). Built with `cargo-ndk` and
linked into the native lib.

### `:daemon` — `astrad`
Standalone C++20 executable (not built by Gradle). Runs as a system service,
listens on `/dev/astra/astrad.sock`, dispatches length-prefixed framed requests
to `CapabilityService` / `ProviderService` / `CommandExecutor`. Phase 0 carries
JSON payloads; Phase 1 migrates to protobuf.

### `:modules` — Astra Module runtime
Manages `.avm` packages (a ZIP: `module.json` + `runtime/arm64.so` + `assets/`
+ `permission.json`). Install → validate → request permissions → unpack →
sandbox profile → (future) `dlopen` the runtime. **This is what distinguishes
AstraVeil from Magisk**: modules are not all-powerful.

### `:sdk` — Public facade
`AstraClient(context)` is the stable surface third-party modules code against.
`getCapability()`, `requestPermission()`, `execute()`. Versioned by an API
level so the contract is stable across releases.

## Data flow: a capability refresh

```
User taps Refresh (AstraUI)
  → StatusViewModel.refresh()
    → AstraCore.refreshCapability()
      → CapabilityEngine.scan()           [reads /proc, /sys, Build]
      → AstraCore.capability = info       [cached for non-suspending readers]
      → EventBus.emit(CapabilityUpdatedEvent(info))
        → StatusViewModel.handleEvent()   [updates StateFlow]
          → Compose recomposes Dashboard
    → ProviderRegistry.detectActive()
      → MagiskProvider.available()        [false on non-rooted]
      → KernelSUProvider.available()
      → APatchProvider.available()
      → AstraRootProvider.available()     [always false in Phase 0]
      → returns null → "None"
    → UiState updated → "Provider: None"
```

## Why the contracts are the way they are

- **`AstraEvent` is open, not sealed.** Sealed interfaces in Kotlin restrict
  subtypes to the same module. AstraVeil needs `:providers` and `:modules` to
  publish their own events, so the interface is plain. The `else` branch in
  consumers is the cost of extensibility.
- **`ProviderRegistry` is an `object`.** Every subsystem needs the active
  backend; a singleton avoids DI plumbing in Phase 0. The `eventBus` is a
  settable var wired once at `Application.onCreate`.
- **Permissions are strings, not an enum.** Providers can introduce new
  capability tokens ("zygisk", "riru", "kprobe") without changing the core
  schema. The `Permission` enum only fixes the baseline tier vocabulary.
- **The daemon is separate from the app.** Long-running privileged work belongs
  in a system service that survives the UI process. The app talks to it over a
  socket; this is the only sustainable design for a root platform.

## Future: AstraRoot & AstraKernel

`AstraRootProvider` is a stub today. When it ships it will not be "another su" —
it will combine **Root Capability + Permission Policy + Module Sandbox** at the
kernel boundary. `AstraKernel` will provide high-performance hooks and security
policy below the userspace daemon. Neither is implemented in Phase 0.
