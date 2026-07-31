# AstraVeil Module Developer Guide

This document is the canonical reference for authoring, signing, and
publishing AstraVeil modules (`.avm` files). If you are writing your first
module, read sections 1–4 in order, then jump to §6 (Signing) before
publishing.

> **TL;DR** — A module is a ZIP with a `module.json` manifest, a native
> runtime binary, optional assets, and (for distribution) three
> `META-INF/ASTRAVEIL.*` signature blocks. It declares capabilities; the
> system decides what sandbox it runs in.

---

## 1. The `.avm` format

An `.avm` ("AstraVeil Module") is a standard ZIP archive. The minimal
unsigned layout is:

```
my-module.avm
├── module.json          # manifest (required, always first)
├── permission.json      # optional, deprecated in v3
├── lib/
│   └── arm64-v8a/
│       └── module.so    # native runtime (ELF aarch64)
├── runtime/
│   └── main             # alternative entrypoint name (POSIX sh or ELF)
└── assets/
    └── …                # arbitrary module data
```

After signing (see §6) the archive additionally contains:

```
├── META-INF/
│   ├── ASTRAVEIL.MF     # manifest of per-file SHA-256 hashes
│   ├── ASTRAVEIL.SIG    # Ed25519 signature over ASTRAVEIL.MF
│   └── ASTRAVEIL.CERT   # base64-encoded Ed25519 public key (X.509 SubjectPublicKeyInfo)
```

### 1.1 Format rules

1. **The ZIP must be uncompressed for `module.json`** so that preview
   parsing can read it without inflating the rest of the archive.
2. **Entry names must not contain `..`** or start with `/`. Paths are
   byte-compared; traversal attempts are rejected at install time.
3. **Only the prefixes `lib/`, `runtime/`, `assets/`, and `META-INF/`**
   are accepted. Unknown top-level entries fail validation.
4. **Total uncompressed size is capped** (default 64 MiB) to prevent zip
   bombs. Larger modules require a special waiver from the AstraHub
   curators.
5. **Symlinks inside the ZIP are stripped** on extract; only regular
   files are honored.

### 1.2 Compatibility with Magisk modules

A Magisk module is **not** a valid `.avm`. Magisk modules lack
`module.json` and the AstraVeil capability model. A conversion shim is
planned for a future release; for now, port Magisk modules manually:
declare each privileged action as an AstraVeil capability.

---

## 2. `module.json` manifest

Two manifest formats are accepted. **Use v3 for new modules.**

### 2.1 v3 manifest (recommended)

```json
{
    "id": "com.example.mytool",
    "name": "My Tool",
    "version": "1.2.0",
    "apiVersion": 3,
    "author": "Jane Developer",
    "description": "One-line description shown in the module list.",
    "permissions": [
        {
            "capability": "system.info",
            "reason": "Read kernel version for compatibility checks.",
            "riskLevel": 5
        },
        {
            "capability": "filesystem.read",
            "reason": "Read user-selected files.",
            "riskLevel": 25
        }
    ]
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | string | yes | Reverse-DNS, lowercase, unique. |
| `name` | string | yes | Human-readable; 1–48 chars. |
| `version` | string | yes | SemVer. |
| `apiVersion` | int | yes | Currently `1`, `2`, or `3`. See §4.2. |
| `author` | string | no | Displayed in UI. |
| `description` | string | no | One line. |
| `permissions` | array | yes | May be empty; see §3. |

### 2.2 Phase-0 manifest (legacy)

```json
{
    "name": "My Tool",
    "version": "1.0.0",
    "api": 1,
    "description": "Legacy Phase-0 module.",
    "permissions": ["system.info"]
}
```

Phase-0 manifests carry **no risk information**; the UI renders risk as
"Unknown". Phase-0 modules are accepted for backward compatibility but
cannot be signed (the signing pipeline requires a v3 manifest). New
modules must use v3.

### 2.3 Optional `sandbox` hint

```json
"sandbox": {
    "filesystem": "restricted",
    "network": false
}
```

The `sandbox` field is **advisory only**. The effective `SandboxProfile`
is always computed by `SandboxPolicyResolver` from the declared
permission set (see §8). The hint is shown to the user as the developer's
stated intent and is compared against the system-derived profile; a
mismatch is surfaced as a warning.

---

## 3. Capability vocabulary

Capabilities are free-form strings, but a baseline vocabulary is defined
so that the permission engine can map them to risk tiers and sandbox
profiles. Unknown capabilities are accepted but rendered as "Unknown
risk" in the UI and treated at the lowest tier.

### 3.1 Tier model

The baseline tiers (see `core/.../permission/Permission.kt`) are:

| Tier | Level | Meaning |
|------|-------|---------|
| `NONE` | 0 | No elevated privileges. |
| `SHELL` | 10 | Unprivileged shell (uid 2000). |
| `ROOT` | 100 | Root (uid 0). |
| `KERNEL` | 1000 | Kernel-level hooks / syscall interposition. |

### 3.2 Capability tokens

| Capability | Tier | Risk | Notes |
|------------|------|------|-------|
| `system.info` | NONE | 5 | Read non-identifying system info (Build, kernel, SELinux state). |
| `filesystem.read` | NONE | 20 | Read files inside the module's data dir + user-selected URIs. |
| `filesystem.write` | NONE | 30 | Write inside the module's data dir. |
| `network.outbound` | SHELL | 40 | Outbound TCP/UDP. |
| `property.set` | ROOT | 60 | Set Android system properties. |
| `mount.bind` | ROOT | 70 | Bind-mount filesystems inside the module's namespace. |
| `namespace.create` | ROOT | 75 | Create new mount/PID namespaces. |
| `su.shell` | ROOT | 90 | Spawn a root shell. **Strict mode forbids this.** |
| `kernel.hook` | KERNEL | 100 | Install kernel-level hooks. Reserved for OFFICIAL modules. |

### 3.3 Declaring capabilities

Each entry in `permissions` must specify:

- `capability`: a token from §3.2 (or a custom one prefixed with your
  reverse-DNS, e.g. `com.example.mytool.custom`).
- `reason`: a human-readable string the user will see in the install
  dialog. Be specific: "Reads `/proc/cpuinfo` to detect big.LITTLE" is
  good; "Needs filesystem" is not.
- `riskLevel`: an integer 0–100. **This is the developer's
  self-assessment.** The system does not trust it; it is shown to the
  user as a sanity check against the system-derived risk.

### 3.4 Custom capabilities

Providers may introduce new capability tokens (e.g. `zygisk`,
`riru.load`, `kprobe.trace`) without changing the core schema. Modules
declaring custom capabilities must:

1. Prefix them with their reverse-DNS to avoid collisions.
2. Document them in the module description.
3. Accept that the user will see them as "Unknown risk".

---

## 4. Module lifecycle

### 4.1 States

A module transitions through the following states (see
`modules/.../lifecycle/ModuleState.kt`):

```
DISCOVERED → STAGED → VALIDATED → APPROVED → INSTALLED → ENABLED
                ↓          ↓           ↓          ↓          ↓
              REJECTED   REJECTED   REJECTED  DISABLED   UNINSTALLED
```

| State | Meaning |
|-------|---------|
| `DISCOVERED` | The `.avm` has been located (e.g. downloaded). |
| `STAGED` | The file has been copied into the staging area. |
| `VALIDATED` | Manifest parsed, signature verified, sandbox profile derived. |
| `APPROVED` | User has approved the requested capabilities. |
| `INSTALLED` | Files extracted to `/data/astra/modules/<id>/`. |
| `ENABLED` | The runtime has been loaded and is ready to execute. |
| `DISABLED` | Installed but not loaded (user toggle). |
| `UNINSTALLED` | Files removed; audit history retained. |
| `REJECTED` | Validation or approval failed; files removed from staging. |

### 4.2 API versions

| `apiVersion` | Status | Notes |
|--------------|--------|-------|
| 1 | Legacy | Phase-0 manifests only; no signing. |
| 2 | Supported | v3 manifest with `riskLevel`; signing optional. |
| 3 | **Current** | v3 manifest; signing required for distribution. |

Unsupported `apiVersion` values fail validation. Bump the API version
when you use a feature not present in older runtimes.

### 4.3 Install flow

```
User selects my-module.avm
  → AvmManifestParser.parse()               # extract module.json
  → UI shows name, version, capabilities, risk
  → User taps Install
  → ModuleSignatureVerifier.verify()         # crypto + content hash
  → TrustGate.evaluate()                     # combines signature + manifest
  → SandboxPolicyResolver.resolve(risk)      # derive SandboxProfile
  → User approval dialog (if any perm is dangerous)
  → Extract to /data/astra/modules/<id>/
  → ModuleRecord persisted in registry
  → State = ENABLED (or DISABLED if user chose so)
```

### 4.4 Update flow

Updates use the same path. The registry compares the new `version` to
the installed one (SemVer) and refuses downgrades unless the user
explicitly opts in. Audit history from the previous version is
preserved.

### 4.5 Uninstall

Uninstall removes the module directory and revokes all its capability
grants. Audit entries are **retained** (marked `module_uninstalled`) so
that post-incident review is possible.

---

## 5. The AstraVeil SDK

Modules written in Kotlin or Java should depend on the `:sdk` artifact.
Native (C++) modules link against `sdk/include/astra/module_api.hpp`.

### 5.1 Gradle dependency

```kotlin
dependencies {
    implementation("com.astraveil:sdk:1.0.0")
}
```

### 5.2 The `AstraClient` facade

```kotlin
class MyModule {
    private val client = AstraClient(context)

    suspend fun readKernelVersion(): String {
        // requestPermission is a no-op if already granted
        client.requestPermission("system.info", reason = "Read kernel version")
        return client.execute("uname -r").stdout.trim()
    }
}
```

Key entrypoints:

| Method | Purpose |
|--------|---------|
| `AstraClient(context)` | Construct the client. Cheap; safe to retain. |
| `requestPermission(cap, reason)` | Ensure a capability is granted. Suspends until the user decides. |
| `execute(command)` | Run a shell command inside the sandbox. Returns `ExecutionResult`. |
| `getCapability(name)` | Read a capability value (e.g. `kernel.version`). |
| `subscribe(event)` | Subscribe to `AstraEvent`s. |

### 5.3 Native API

```cpp
#include <astra/module_api.hpp>

extern "C" int astra_module_main(astra::ModuleContext& ctx) {
    auto perm = ctx.requestPermission("system.info", "Read kernel info");
    if (!perm.granted) return 1;
    auto out = ctx.execute("uname -r");
    ctx.log(astra::LogLevel::Info, "kernel: %s", out.stdout.c_str());
    return 0;
}
```

The native API is ABI-stable across `apiVersion` bumps within a major
version. See `sdk/include/astra/module_api.hpp` for the full reference.

### 5.4 What the SDK does **not** expose

- Direct access to `astrad.sock`. Use `AstraClient.execute()`.
- Raw `su` invocation. Use the `su.shell` capability (strict mode
  forbids this).
- Filesystem access outside your data dir. Use `filesystem.read` /
  `filesystem.write` and content URIs.

---

## 6. Signing modules

Distribution requires a valid Ed25519 signature. Unsigned modules can
only be installed in developer mode and are clearly flagged.

### 6.1 Generate a keypair

Use the bundled CLI in `tools/avm-sign/`:

```bash
kotlinc -script tools/avm-sign/AvMSigner.kt -- keygen myname
# → writes myname.priv.pem and myname.pub.pem
```

Keep `myname.priv.pem` **offline**. If you lose it, you cannot ship
updates to the same module id under the same trust level.

### 6.2 Sign a module

```bash
kotlinc -script tools/avm-sign/AvMSigner.kt -- sign my-module.avm myname.priv.pem "Jane Developer"
# → writes my-module.signed.avm
```

The signer produces three new entries inside the archive:

1. `META-INF/ASTRAVEIL.MF` — a manifest listing every entry's SHA-256.
2. `META-INF/ASTRAVEIL.SIG` — an Ed25519 signature over the manifest
   bytes.
3. `META-INF/ASTRAVEIL.CERT` — your base64-encoded public key.

### 6.3 Verify locally

```kotlin
val verification = ModuleSignatureVerifier.verify(
    avmFile = File("my-module.signed.avm"),
    officialPublicKeyB64 = OFFICIAL_KEY_B64,
    trustedKeys = developerKeyStore.trustedKeySet(),
)
when (verification.trustLevel) {
    TrustLevel.OFFICIAL          -> /* … */
    TrustLevel.TRUSTED_DEVELOPER -> /* … */
    TrustLevel.UNKNOWN_DEVELOPER -> /* warn user */
    TrustLevel.UNSIGNED          -> /* refuse in non-dev mode */
    TrustLevel.INVALID           -> /* refuse always */
}
```

### 6.4 Key rotation

If your private key is compromised:

1. Generate a new keypair.
2. Sign the next version of your module with the new key.
3. Notify users: the new version will install as `UNKNOWN_DEVELOPER`
   until they re-trust the new fingerprint.
4. If you are an OFFICIAL signer, also coordinate with the AstraVeil
   security team to publish a key rotation in the remote config (the
   old public key is added to a revocation list).

### 6.5 Official signing

The AstraVeil official keypair is held by the project maintainers and
used only for first-party modules. Third-party developers cannot obtain
OFFICIAL trust level; the highest achievable level is
`TRUSTED_DEVELOPER`, granted by users who explicitly trust your public
key.

---

## 7. Trust levels

`ModuleSignatureVerifier` produces one of five trust levels. The UI
surfaces them as follows:

| Level | Badge color | Installable? | Notes |
|-------|-------------|--------------|-------|
| `OFFICIAL` | Green | Yes | Signed by the AstraVeil official key. |
| `TRUSTED_DEVELOPER` | Blue | Yes | Signed by a key in the user's trusted-developer set. |
| `UNKNOWN_DEVELOPER` | Yellow | Yes (with warning) | Validly signed by an unrecognized key. |
| `UNSIGNED` | Gray | Dev mode only | No signature block present. |
| `INVALID` | Red | No | Signature verification or content hash failed. |

### 7.1 Trusting a developer

Users add a developer to their trusted set via the module detail screen:

1. Install a module signed by the developer (it will be
   `UNKNOWN_DEVELOPER`).
2. Open the module detail screen.
3. Tap "Trust this developer".
4. Confirm the fingerprint matches the one the developer publishes out
   of band (e.g. on their website, in a signed README).

The trust grant is persisted by `DeveloperKeyStore` in
`trusted_developers.json` (app-private storage, `0600`).

### 7.2 Revoking trust

From the same screen, "Revoke trust" removes the key. All currently
installed modules signed by that key are disabled; they may be
re-enabled only by re-trusting or by an update signed by a different
(trusted) key.

---

## 8. Sandbox constraints

Every enabled module runs inside a `SandboxProfile` derived by
`SandboxPolicyResolver`. The profile is computed from the **system's**
assessment of the module's risk, not the manifest's declared
`riskLevel`.

### 8.1 Risk → profile mapping

| Effective risk | Filesystem | Namespace | Property | Approval |
|----------------|------------|-----------|----------|----------|
| < 30 | denied | denied | denied | Automatic |
| 30–69 | allowed (in data dir) | allowed | denied | Required if any tier ≥ ROOT |
| ≥ 70 | allowed | allowed | allowed | Required; "are you sure" gate |

### 8.2 What the sandbox enforces

- **Mount namespace isolation.** The module sees only its own data dir
  and explicitly granted mounts.
- **Seccomp BPF filter.** `fork`/`clone` are rate-limited; `ptrace`,
  `kexec_load`, `bpf` (unprivileged variant) are denied.
- **Landlock (where supported).** Filesystem access is restricted to
  the module's data dir + read-only system paths.
- **SELinux domain.** Module processes run in `astrad_module` domain,
  which is more restrictive than `astrad`.
- **CPU/wall-time quota.** Enforced by `ModuleWatchdog`. Exceeding the
  quota kills the sandbox and emits an audit entry.

### 8.3 What the sandbox does **not** enforce (yet)

- Network egress filtering. `network.outbound` is a binary grant; per-host
  filtering is planned for a future release.
- Memory quota. The kernel OOM killer is the only backstop today.
- Per-call CPU quota. The watchdog operates on wall-time windows.

### 8.4 Opting into stricter isolation

Modules that do not need filesystem or namespace access should declare
only low-tier capabilities; the resolver will then grant the locked-down
profile automatically. There is no manual "strict mode" flag — least
privilege is the default.

---

## 9. Testing your module

### 9.1 Local smoke test

```bash
# 1. Build the .avm
./gradlew :my-module:assembleAvm

# 2. Sign it
kotlinc -script tools/avm-sign/AvMSigner.kt -- sign build/outputs/my-module.avm dev.priv.pem "Me"

# 3. Verify locally
./tools/avm-cli/avm verify my-module.signed.avm
```

### 9.2 Required tests

Every module submission to AstraHub must include:

1. **A unit test** that loads the manifest and asserts the declared
   capabilities match the runtime's actual calls.
2. **A sandbox test** that runs the module inside the AstraVeil sandbox
   and asserts it does not access paths outside its data dir.
3. **A signature round-trip test**: sign, verify, tamper one byte,
   assert verification fails.

### 9.3 Adversarial self-test checklist

Before publishing, verify:

- [ ] The module runs correctly with **only** the declared capabilities.
- [ ] Removing any declared capability causes a clean failure, not a
      crash.
- [ ] The module does not write outside its data dir (run with
      `strace`/`ltrace` if needed).
- [ ] The module does not spawn long-lived background processes.
- [ ] The module handles `SIGTERM` cleanly (the watchdog sends it
      before `SIGKILL`).
- [ ] The signed artifact still passes `avm verify` after being
      re-zipped (i.e. you did not accidentally include absolute paths
      or symlinks).

### 9.4 CI

AstraHub's CI runs the same checks on submission. Failures are reported
with the specific entry that failed. Re-running CI after a fix is free
and unlimited.

---

## 10. Publishing

### 10.1 AstraHub

AstraHub (`astrahub/modules/index.json` in this repo, mirrored to a CDN
for production) is the canonical discovery surface. To publish:

1. Fork the repo.
2. Add an entry to `astrahub/modules/index.json`:
   ```json
   {
     "id": "com.example.mytool",
     "name": "My Tool",
     "version": "1.2.0",
     "author": "Jane Developer",
     "category": "system-tools",
     "permissions": ["system.info", "filesystem.read"],
     "security_score": 75,
     "risk_level": "LOW",
     "description": "One-line description."
   }
   ```
3. Host the signed `.avm` on a stable URL (GitHub Releases, your own
   server, etc.).
4. Open a PR. CI will:
   - Validate the JSON schema.
   - Download the `.avm` and run `ModuleSignatureVerifier.verify()`.
   - Run the sandbox smoke test.
   - Lint the manifest.
5. On merge, the index is published to the CDN within 1 hour.

### 10.2 Categories

| Category | Use for |
|----------|---------|
| `system-tools` | Utilities that read or modify system state. |
| `customization` | Theming, fonts, sounds. |
| `performance` | CPU governors, schedulers, memory tweaks. |
| `security` | Hardening, audit, monitoring. |

### 10.3 Versioning

- Bump the patch version for bug fixes.
- Bump the minor version for new capabilities.
- Bump the major version when the `apiVersion` changes.
- AstraHub refuses to publish a version lower than the highest
  previously published version for the same `id`.

### 10.4 Takedown

If a published module is found to be malicious:

1. The AstraHub curators remove the entry from `index.json`.
2. A revocation entry is added to `astrahub/revoked.json` containing
   the module id and the affected versions.
3. The AstraVeil app fetches `revoked.json` on every refresh and
   disables any installed module whose id+version matches.
4. If the module was signed by a trusted-developer key, that key is
   also added to the revoked-key list and removed from
   `trusted_developers.json` on every device on the next refresh.

---

## Appendix A — Example: a complete minimal module

`module.json`:
```json
{
    "id": "com.example.hello",
    "name": "Hello AstraVeil",
    "version": "1.0.0",
    "apiVersion": 3,
    "author": "Jane Developer",
    "description": "Prints the kernel version. Demo only.",
    "permissions": [
        {
            "capability": "system.info",
            "reason": "Read kernel version.",
            "riskLevel": 5
        }
    ]
}
```

`runtime/main` (POSIX sh):
```sh
#!/system/bin/sh
uname -r
```

Build & sign:
```bash
zip -r hello.avm module.json runtime/
kotlinc -script tools/avm-sign/AvMSigner.kt -- sign hello.avm dev.priv.pem "Jane Developer"
./tools/avm-cli/avm verify hello.signed.avm
```

---

## Appendix B — Reference: file locations

| Artifact | Path |
|----------|------|
| Manifest parser | `core/src/main/java/com/astraveil/core/modules/manifest/AvmManifestParser.kt` |
| Permission model | `core/src/main/java/com/astraveil/core/permission/Permission.kt` |
| Sandbox profile | `modules/src/main/java/com/astraveil/modules/runtime/sandbox/SandboxProfile.kt` |
| Sandbox policy resolver | `modules/src/main/java/com/astraveil/modules/runtime/sandbox/SandboxPolicyResolver.kt` |
| Signature verifier | `modules/src/main/java/com/astraveil/modules/security/ModuleSignatureVerifier.kt` |
| Developer key store | `modules/src/main/java/com/astraveil/modules/security/DeveloperKeyStore.kt` |
| Trust gate | `modules/src/main/java/com/astraveil/modules/security/TrustGate.kt` |
| Signing CLI | `tools/avm-sign/AvMSigner.kt` |
| avm CLI | `tools/avm-cli/avm` |
| Example module | `examples/hello-world.avm/` |
| Module API (C++) | `sdk/include/astra/module_api.hpp` |
| Update verifier (APK) | `app/src/main/java/com/astraveil/app/update/UpdateVerifier.kt` |

---

## Appendix C — FAQ

**Q: Can I ship a module that needs `su.shell`?**
A: Technically yes, but it will be refused in strict mode and the UI
will display a prominent warning. Most use cases for `su.shell` are
better expressed as a specific capability (`mount.bind`,
`property.set`, etc.).

**Q: My module needs to read `/data/data/com.other.app/…`. Can it?**
A: No. The sandbox restricts filesystem access to your own data dir.
Cross-app data access requires the user to grant a content URI through
the standard Android sharing flow.

**Q: How do I update my signing key?**
A: See §6.4. Ship a new version signed with the new key; users will
need to re-trust the new fingerprint.

**Q: What happens if my module crashes?**
A: `ModuleWatchdog` kills the sandbox, the state transitions to
`DISABLED`, and an `AuditEntry` with `OUTCOME_CRASHED` is written. The
user is notified. The module is not auto-restarted; the user must
re-enable it.

**Q: Can I distribute my module outside AstraHub?**
A: Yes. The signed `.avm` is self-contained; users can sideload it.
AstraHub is the discovery surface, not a gatekeeper. Sideloaded modules
still pass through `ModuleSignatureVerifier` at install time.

**Q: How big can my module be?**
A: 64 MiB uncompressed by default. Larger modules need a waiver;
contact the AstraHub curators via a GitHub issue.

---

*This guide is versioned with the AstraVeil platform. The
`apiVersion` field in your manifest must match a version documented
here. When in doubt, check `docs/ROADMAP.md` for upcoming changes.*
