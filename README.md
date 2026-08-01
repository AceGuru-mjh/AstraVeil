<div align="center">

# AstraVeil

**Android Root Capability Operating Layer**

Abstract root backends (Magisk / KernelSU / APatch) behind a unified
capability matrix, a Rust security policy, and structured daemon IPC.

`v2.2.0` · Alpha

</div>

---

## What is AstraVeil?

AstraVeil is **not** another root tool. It is a **capability operating layer**:
it discovers which root backend is present, probes what the device + kernel +
SELinux actually allow, and exposes the result as a **capability matrix** that
modules and the UI read instead of guessing.

> **AstraVeil does not create root. AstraVeil abstracts root capabilities.**

| | Root tool | AstraVeil |
|---|---|---|
| Question it answers | "is there root?" | "what can the device do?" |
| Module trust | module = root | module runs isolated, root is brokered |
| Backend coupling | hardcoded | pluggable `RootProvider` |
| Security model | root = all permissions | Kotlin intent → Rust policy → C++ execution |

---

## Features

### Real Root Management (Magisk)
- **Superuser policy management** — read/write Magisk's `policies` table via
  `magisk --sqlite`. Allow/Ask/Deny changes take effect immediately.
- **Temporary grants** — timed authorization (5min/15min/1h/8h) using
  Magisk's native `until` column; auto-revokes on expiry.
- **One-click lockdown** — instantly deny ALL apps' su requests, with
  one-click restore to pre-lockdown state.
- **Risk-based grouping** — policies auto-grouped by attention score
  (recency + frequency + policy risk); frequent root users float to top.
- **Per-app usage stats** — today's count, last-used time, activity dots,
  recent request history per app.

### Persistent Terminal
- **Live shell session** — `cd`/`export` persist across commands (not
  one-shot `su -c` per command).
- **Streaming output** — line-by-line real-time, not buffered.
- **Live cwd** — status bar shows current working directory, updated per
  command.
- **Interrupt** — kill the running shell mid-command.
- **Three modes** — ROOT (`su`), ADB (`su 2000`), SHELL (app-uid).
- **Audited** — every privileged command logged to an append-only JSONL
  audit trail.

### Root Chain Self-Test
Built-in diagnostic that runs the 7 links of the root chain on-device and
tells you **exactly which link breaks**:
1. Root backend detection
2. `su` binary reachable
3. One-shot `su -c id` executes
4. Output contains `uid=0` (real root)
5. AstraVeil authorized in backend
6. Persistent shell starts and stays alive
7. Marker round-trip (command → output + exit code + cwd)

Each step shows real command output as evidence; failures include actionable
hints.

### AstraHub Module Repository
Curated module index with transport-integrity verification:
- Browse/search community modules
- Download with SHA-256 verification against the index
- Hand-off to the installer (TrustGate enforces signature + capability check)

### Security Model
```
User          (intent: authorise)
  ↓
AstraUI       (interaction, Compose)
  ↓
AstraCore     (judgement + coordination, Kotlin)
  ↓
Rust Policy   (Allow / Deny / RequireApproval + risk)  ← fail-closed
  ↓
AstraDaemon   (execution only, C++20)
  ↓
RootProvider  (Magisk / KernelSU / APatch)
  ↓
Kernel        (hardware, fully untrusted)
```

- **Fail-closed**: if the Rust policy engine is not linked, ALL execution is
  denied (weak fallback returns `Deny`, not `Allow`).
- **Structured IPC**: daemon Execute requests carry `moduleId` /
  `capability` / `riskLevel` / `approved` / `caller` — raw command strings
  are never the public API (audit P0-1).
- **Unified execution**: `ExecutionRouter` is the single non-interactive
  execution entry point. Daemon-only, no `su` fallback. Interactive terminal
  is a separate, explicitly-approved channel (P1-12).
- **TrustGate**: module install requires verified signature (`strict=true`).
- **TOCTOU protection**: single staging file + hash re-verification before
  install.
- **Zip Slip protection**: canonical path validation + zip bomb limits.
- **Native module policy**: third-party native code refused in-process until
  isolated ModuleRunner (Phase 1).

### Diagnostics with Provenance
Every diagnostic conclusion carries its **source** — how it was determined:
- Device facts: `DETECTED` from `android.os.Build`, `/proc`, `/sys`
- Capabilities: `PROBED` via real syscall tests (`unshare`, `getuid`)
- Provider status: `DETECTED` via file existence + functional binary check
- Subsystem state: honestly marked `Prototype` / `Unavailable` when not
  implemented (no inflated claims)

---

## Module Layout

```
AstraVeil/
├── app/         # AstraUI — Compose control center + terminal + superuser
├── core/        # AstraCore — capability / permission / config / IPC protocol
├── providers/   # RootProvider abstraction + Magisk / KernelSU / APatch
├── modules/     # .avm module runtime (install / trust gate / sandbox / compatibility)
├── native/      # C++20 JNI bridge
├── rust/        # Rust security crate (policy engine, fail-closed)
├── daemon/      # astrad — C++20 system service (structured IPC, real probes)
├── sdk/         # Public facade for third-party .avm modules
├── docs/        # Architecture, threat model, developer guide
└── astrahub/    # Module repository index
```

---

## Build

> Requires JDK 17, Android SDK (API 26+), and optionally NDK + cargo-ndk
> for the daemon and Rust components.

```bash
# Build the debug APK
./gradlew :app:assembleDebug

# Build the daemon (Linux host with C++20 toolchain)
cmake -S daemon -B daemon/build && cmake --build daemon/build

# Build the Rust policy crate for arm64
cd rust && cargo ndk -t arm64-v8a -p 26 build --release
```

### Signed Release

Release APKs are signed with a keystore stored as an encrypted GitHub secret
(`ASTRAVEIL_KEYSTORE_BASE64`). The keystore is **never** committed to the repo.
CI decodes it at build time and verifies the APK is signed before publishing.

---

## Current Status — Alpha (v2.2.0)

### What works (no daemon required)
- ✅ Device/capability display (real probes: `/proc`, `/sys`, `Build`)
- ✅ Root backend detection (Magisk / KernelSU / APatch)
- ✅ Superuser policy management (Magisk only — real DB read/write)
- ✅ Persistent terminal (ROOT / ADB / SHELL modes, cd/export persist)
- ✅ Root Chain Self-Test (7-link diagnostic)
- ✅ AstraHub module browsing + download + SHA-256 verification
- ✅ Module install (TrustGate + Zip Slip + TOCTOU protection)
- ✅ Settings (theme/language/notifications — actually persist and apply)
- ✅ Update system (check / download / verify signature / install)
- ✅ Backup/restore (export/import registry + keys + audit log via SAF)

### What requires daemon (Phase 1)
- ⏳ Module isolation (isolated process via daemon fork + dlopen)
- ⏳ Real-time su request interception
- ⏳ Sandbox enforcement (seccomp + landlock + namespace)
- ⏳ Systemless mounts (overlayfs — the core value of modules)
- ⏳ Unified execution via daemon (ExecutionRouter is wired; daemon needs
  real-device deployment)

### Honest limitations
- Superuser management is **Magisk-only** (KernelSU/APatch don't have
  `magisk --sqlite`; cross-backend su policy DB is Phase 1).
- Terminal does not support full-screen interactive programs (`top`/`vi`/
  `less`) — that requires a real PTY (Phase 2, native).
- The daemon (`astrad`) has never been deployed on a real device. All daemon
  code compiles and passes CI, but the end-to-end App↔daemon↔root chain has
  not been verified on hardware yet.

---

## Releases

| Version | Highlights |
|---|---|
| v2.2.0 | Superuser dashboard (lockdown + risk grouping + temp grants), daemon real probes |
| v2.1.0 | Persistent terminal, Root Chain Self-Test, P2-18 diagnostics provenance |
| v2.0.0 | Settings consolidation (12→10), theme preference actually works |
| v1.9.0 | Settings wiring (9 real screens), liquid glass cleanup, capability UI |
| v1.8.0 | Signed APK pipeline (base64 keystore secret + verify) |

Download from the [Releases page](https://github.com/AceGuru-mjh/AstraVeil/releases).

---

## Technology Stack

| Layer | Tech |
|---|---|
| UI | Kotlin + Jetpack Compose + Material 3 |
| Core / SDK | Kotlin 2.0, kotlinx-serialization |
| Daemon | C++20, CMake, nlohmann/json |
| Security | Rust (policy engine, fail-closed via weak symbol) |
| IPC | Structured JSON frames over Unix Domain Socket |
| Native bridge | C++20 + JNI |

---

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Threat Model](docs/THREAT_MODEL.md)
- [Module Developer Guide](docs/MODULE_DEVELOPER_GUIDE.md)
- [AstraHub Submission](docs/ASTRAHUB_SUBMISSION.md)
- [Roadmap](docs/ROADMAP.md)

---

## License

Proprietary — AstraVeil Project (MJH). See [LICENSE](LICENSE).

Third-party modules (`.avm`) developed against the AstraVeil SDK are the
property of their authors and are not governed by this license.
