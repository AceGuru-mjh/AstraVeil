# AstraVeil Threat Model

**Status:** Living document — owned by the AstraVeil security working group.
**Scope:** All AstraVeil components shipped in this repository (`:app`, `:core`,
`:modules`, `:providers`, `:sdk`, `:native`, `:rust`, `:daemon`, `tools/`,
`magisk-module/`, `selinux/`).
**Methodology:** STRIDE / attack-tree driven, aligned with NIST SP 800-154
guidance for mobile root platforms. Every finding below has a mitigation owner
and a verification step listed in §7.

---

## 1. Scope

### 1.1 In scope

| Asset | Location | Why it matters |
|-------|----------|----------------|
| AstraVeil APK | `:app` | Control center; if tampered, attacker controls the user's root policy. |
| Update packages | served via GitHub Releases | A backdoored update compromises every device that installs it. |
| `astrad` daemon binary | `:daemon` | Runs as a privileged service; the trust root for module execution. |
| `.avm` module packages | `:modules`, `examples/*.avm`, AstraHub | Third-party code that runs *inside* the platform's sandbox. |
| Module signing keys | developer machines, `tools/avm-sign` | Whoever holds the official Ed25519 key can ship "OFFICIAL" modules. |
| Trusted-developer key store | `trusted_developers.json` (per-device) | If poisoned, a malicious module becomes "TRUSTED_DEVELOPER". |
| SELinux policy | `selinux/*.te`, `file_contexts` | The last line of defense if a sandbox escape occurs. |
| IPC socket | `/dev/astra/astrad.sock` | Any reachable peer can request privileged operations. |
| Capability / permission grants | `:core` permission engine | Mis-grants turn a sandboxed module into root. |

### 1.2 Out of scope

- Vulnerabilities in upstream root providers (Magisk, KernelSU, APatch). These
  are tracked upstream; AstraVeil only consumes their `RootProvider` contract.
- Generic Android OS vulnerabilities (kernel, ART, framework).
- Social-engineering attacks against end users (e.g. phishing for ADB keys).
- Physical attacks requiring device seizure *and* bootloader unlock — out of
  our threat model once the attacker holds the bootloader keys.

### 1.3 Security objectives

1. **Integrity of self.** AstraVeil's own binaries, daemon, and configuration
   may only be modified by packages that pass checksum **and** signature
   verification against the installed app's signing certificate
   (`UpdateVerifier`).
2. **Integrity of modules.** A `.avm` may only run after cryptographic
   signature verification (`ModuleSignatureVerifier`) and content-hash
   manifest validation.
3. **Least privilege.** Every module runs inside a derived `SandboxProfile`
   whose capability ceiling is computed from the manifest's declared risk —
   the module does *not* choose its own profile.
4. **Auditability.** Every privileged operation emits an `AuditEntry`; the
   log is append-only and accessible to the user.
5. **Revocability.** Any trust grant (permission, trusted-developer key,
   installed module) can be revoked by the user with immediate effect.

---

## 2. Trust boundaries

```
   ┌────────────────────────────────────────────────────────────────┐
   │  TB-0  Internet                                                 │
   │      │  GitHub Releases · AstraHub index · developer sites      │
   │      ▼                                                           │
   │  TB-1  AstraUI process (unprivileged, uid=app)                 │
   │      │  in-process AstraCore facade                              │
   │      ▼                                                           │
   │  TB-2  astrad socket boundary  (UDS, SELinux domain astrad)    │
   │      │                                                           │
   │  TB-3  astrad daemon (privileged service)                      │
   │      │  ├─ ModuleSignatureVerifier (verify before unpack)       │
   │      │  ├─ SandboxManager (landlock/seccomp/ns isolation)       │
   │      │  └─ CommandExecutor → RootProvider                       │
   │      ▼                                                           │
   │  TB-4  Per-module sandbox (separate mount ns + seccomp)         │
   │      │                                                           │
   │      ▼                                                           │
   │  TB-5  RootProvider backend (Magisk/KernelSU/APatch/AstraRoot) │
   │      │                                                           │
   │      ▼                                                           │
   │  TB-6  Kernel (Linux/AstraKernel future)                        │
   └────────────────────────────────────────────────────────────────┘
```

| Boundary | Trust assumption | Cross-boundary check |
|----------|------------------|----------------------|
| TB-0 → TB-1 | "The network may deliver anything." | TLS, SHA-256 checksum, **`UpdateVerifier.verifySignatureMatches`** compares APK signing cert to the *installed* app's cert. |
| TB-1 → TB-2 | "The UI process is unprivileged and may be compromised." | Peer authentication on UDS; daemon rejects unauthenticated peers; SELinux `astrad` domain restricts which contexts may `connect()`. |
| TB-2 → TB-3 | "The daemon is the trust root for module execution." | n/a (internal to daemon). |
| TB-3 → TB-4 | "A module's bytecode/native lib is untrusted until signature-verified." | `ModuleSignatureVerifier.verify()` + manifest hash check before any entry is read. |
| TB-4 → TB-5 | "Sandboxed module code is untrusted." | Capability tokens passed through `PermissionEngine`; provider only honors grants it can re-validate. |
| TB-5 → TB-6 | "The root provider may have its own bugs." | Defense-in-depth: SELinux policy + (future) AstraKernel policy gates. |

### 2.1 Trust roots

- **R1: Installed app signing certificate.** Used by `UpdateVerifier` to
  accept or reject update APKs. Stored by Android; we read it via
  `PackageManager.GET_SIGNING_CERTIFICATES` (API 28+) with a legacy
  `GET_SIGNATURES` fallback.
- **R2: AstraVeil official Ed25519 public key.** Compiled into the daemon.
  A `.avm` whose `META-INF/ASTRAVEIL.CERT` matches this key receives
  `TrustLevel.OFFICIAL`.
- **R3: User-curated trusted-developer key set.** Persisted in
  `trusted_developers.json` on device. Keys here yield
  `TrustLevel.TRUSTED_DEVELOPER`. The user can add or revoke at any time.
- **R4: Per-operation capability grants.** Time-bounded; cleared on reboot.

### 2.2 Things explicitly NOT trusted

- The `module.json` *contents* are not trusted until the manifest is
  verified against the `.SIG` file inside the `.avm`.
- File names inside the `.avm` are not trusted — paths are compared
  byte-for-byte and reject `..` traversal.
- Risk levels declared in `module.json` are treated as advisory; the
  system recomputes the effective `SandboxProfile` via
  `SandboxPolicyResolver`.
- The AstraHub index is **not** trusted for code; it is only a discovery
  surface. Even indexed modules must pass signature verification at
  install time.

---

## 3. Threat actors

| Actor | Capability | Motivation | Likelihood |
|-------|------------|------------|------------|
| **A1 — Malicious module author** | Writes and distributes `.avm` packages; may try to obtain "TRUSTED_DEVELOPER" status. | Privilege escalation, data exfiltration, persistence. | High |
| **A2 — Network attacker** | MITM between device and GitHub Releases / AstraHub / developer site. | Distribute a trojanized update or module. | Medium (TLS raises cost) |
| **A3 — Compromised update mirror** | Controls a CDN or mirror that the app consults. | Same as A2 but stronger. | Low–Medium |
| **A4 — Compromised developer machine** | Holds the official signing key. | Ship a backdoored OFFICIAL module/update. | Low but catastrophic |
| **A5 — Local malicious app** | Runs on the same device, unprivileged. | Reach `astrad.sock`; escalate to root via AstraVeil. | Medium |
| **A6 — Rooted-device adversary with shell** | Has ADB or a root shell on the user's device. | Disable auditing, plant modules, disable sandbox. | Medium (user's own device) |
| **A7 — Insider with repo access** | Can commit to `feat/*` branches, push tags. | Backdoor the source. | Low |
| **A8 — Nation-state / advanced persistent threat** | All of the above + zero-days. | Targeted surveillance. | Low for most users; out of practical scope beyond best-effort. |

### 3.1 Actor → objective matrix

| Objective | A1 | A2 | A3 | A4 | A5 | A6 | A7 |
|-----------|----|----|----|----|----|----|----|
| Run unsigned code in module sandbox | ✓ | ✓ | ✓ | — | — | ✓ | ✓ |
| Obtain OFFICIAL trust level | — | — | — | ✓ | — | — | ✓ |
| Tamper with an in-flight update | — | ✓ | ✓ | — | — | — | — |
| Reach root via astrad | ✓ | — | — | — | ✓ | ✓ | ✓ |
| Disable auditing silently | — | — | — | — | — | ✓ | ✓ |

---

## 4. STRIDE analysis

We apply STRIDE per trust boundary. Each row maps to one or more controls
and one or more tests in §7.

### 4.1 Spoofing

| ID | Threat | Boundary | Mitigation | Test |
|----|--------|----------|------------|------|
| S-1 | A malicious update APK spoofs the AstraVeil package name | TB-0 → TB-1 | `UpdateVerifier.verifySignatureMatches`: APK signing cert must match installed app's cert (SHA-256 of DER). | `UpdateVerifierTest: rejectsApkWithWrongSigner` |
| S-2 | A local app impersonates AstraUI on the UDS | TB-1 → TB-2 | Peer-auth on UDS; SELinux `astrad` domain only accepts connections from `astraveil_app` context. | `test_peer_auth.cpp` |
| S-3 | A `.avm` spoofs an OFFICIAL module by reusing a public cert blob | TB-3 → TB-4 | `ModuleSignatureVerifier` requires a valid Ed25519 signature over the manifest; cert alone is insufficient. | `ModuleSignatureVerifierTest: rejectsReplayedCertWithoutSig` |
| S-4 | A module claims a different signer name in `module.json` than the signing cert | TB-3 → TB-4 | The displayed signer is the one extracted from the signed manifest (`Signer:` line), not the unsigned `module.json`. | UI snapshot test |

### 4.2 Tampering

| ID | Threat | Boundary | Mitigation | Test |
|----|--------|----------|------------|------|
| T-1 | Network attacker flips bytes in a downloaded APK | TB-0 → TB-1 | SHA-256 checksum from trusted channel; signature match. | `UpdateVerifierTest: rejectsFlippedByte` |
| T-2 | Attacker modifies a file inside a `.avm` after signing | TB-3 → TB-4 | Per-file SHA-256 entries in `ASTRAVEIL.MF`; verifier recomputes and compares. | `ModuleSignatureVerifierTest: detectsTamperedEntry` |
| T-3 | Attacker swaps the manifest+signature pair from an old release onto a new payload | TB-3 → TB-4 | Manifest enumerates every entry's hash; missing/extra entries fail verification. | `ModuleSignatureVerifierTest: rejectsStaleManifestOnNewPayload` |
| T-4 | Attacker edits `trusted_developers.json` directly on disk | TB-4 (on-device) | File lives in `context.filesDir` (app-private, SELinux-enforced); integrity re-checked on load. | Instrumented test with root-injected file |
| T-5 | Attacker replaces `astrad` binary on disk | TB-2 → TB-3 | Daemon binary is verified by Android package signature (it ships inside the APK); Magisk-module-style side-loading is rejected. | `AstraRootProviderTest` |

### 4.3 Repudiation

| ID | Threat | Boundary | Mitigation | Test |
|----|--------|----------|------------|------|
| R-1 | Module performs privileged action, then denies it | TB-4 → TB-3 | Every privileged call writes an `AuditEntry` (module id, capability, timestamp, outcome). | `AuditLoggerTest` |
| R-2 | User revokes a key but the action it authorized is later denied to have happened | TB-4 | Audit log entries are immutable; revocation does not rewrite history. | `AuditLoggerTest: revocationDoesNotEraseHistory` |
| R-3 | A daemon crash loses the last audit entries | TB-3 | Audit log is flushed before privileged operations return; on restart the log is replayed and any incomplete entry is finalized as `OUTCOME_CRASHED`. | `crash_guard` integration test |

### 4.4 Information disclosure

| ID | Threat | Boundary | Mitigation | Test |
|----|--------|----------|------------|------|
| I-1 | Module reads another module's data dir | TB-4 | Per-module mount namespace + `SandboxProfile.filesystem=false` for risk<30; directories are `0700` and named by module id. | `SandboxValidatorTest` |
| I-2 | Module reads device identifiers | TB-4 | No identifier capability is granted by default; `system.info` is tier-restricted. | `PermissionEngineTest` |
| I-3 | Daemon log leaks secrets | TB-3 | `AstraLogger` redacts known-secret patterns; logs are ring-buffered and never persisted to disk in plaintext. | `AstraLoggerTest: redactsSecrets` |
| I-4 | Update channel leaks the user's device fingerprint | TB-0 → TB-1 | Update checks send only `app version` + `channel`; no GAIDs, no IMEI. | Update-network capture test |

### 4.5 Denial of service

| ID | Threat | Boundary | Mitigation | Test |
|----|--------|----------|------------|------|
| D-1 | Malicious `.avm` is a zip bomb | TB-3 → TB-4 | `ModuleSignatureVerifier` and `AvmManifestParser` cap total uncompressed bytes; `ZipInputStream` is walked without materializing unknown entries. | `ModuleValidatorTest: rejectsZipBomb` |
| D-2 | A module forks/execs to exhaust PIDs | TB-4 | Seccomp filter denies `fork`/`clone` beyond an rlimit; `ModuleWatchdog` kills the sandbox if CPU/wall time exceeds quota. | `ModuleWatchdogTest` |
| D-3 | Local app floods `astrad.sock` with requests | TB-1 → TB-2 | Per-peer rate limit + token bucket; abusive peers are temporarily disconnected. | `socket_server` integration test |
| D-4 | A signing key is leaked and attacker floods AstraHub with OFFICIAL-looking modules | TB-0 → TB-1 | AstraHub re-verifies signatures server-side; revoked keys propagate via index `revoked` list. | Hub-side pipeline test |

### 4.6 Elevation of privilege

| ID | Threat | Boundary | Mitigation | Test |
|----|--------|----------|------------|------|
| E-1 | Module's declared risk is lower than its real risk to get a laxer sandbox | TB-3 → TB-4 | `SandboxPolicyResolver` derives the effective profile from the **permission set**, not the manifest's `risk` field; declared risk is UI-only. | `SandboxPolicyResolverTest` |
| E-2 | Module requests `ROOT` permission by lying in `module.json` | TB-3 → TB-4 | Dangerous permissions require explicit user approval through the UI; the daemon re-validates the grant on every privileged call. | `PermissionEngineEdgeCasesTest` |
| E-3 | Path traversal in `.avm` entries (e.g. `../../etc/passwd`) | TB-3 → TB-4 | Entry names are rejected if they contain `..` or start with `/`; only allowlisted prefixes (`lib/`, `assets/`, `runtime/`) are accepted. | `ModuleValidatorTest: rejectsTraversal` |
| E-4 | Module exploits a daemon bug to escape its sandbox | TB-4 → TB-3 | Defense in depth: landlock + seccomp + mount ns + SELinux `astrad` domain. | `sandbox` integration tests |
| E-5 | Stale capability token reused after revocation | TB-3 | Capability tokens carry an expiry; revoked tokens are added to an in-memory denylist that is checked on every call. | `CapabilityServiceTest` |

---

## 5. Attack trees

### 5.1 Attacker goal: ship a malicious "OFFICIAL" module

```
GOAL: malicious .avm runs with TrustLevel.OFFICIAL
├── 1. Compromise official Ed25519 private key
│   ├── 1.1 Steal from developer machine
│   │   └── Mitigation: key lives in HSM/YubiKey; CI never sees private key
│   └── 1.2 Compromise CI signing step
│       └── Mitigation: signing is offline; CI emits unsigned artifacts
├── 2. Forge an Ed25519 signature without the key  [infeasible — 2^128]
├── 3. Replay an old official signature onto a new payload
│   └── Mitigation: manifest hashes cover every byte → fails (T-3)
└── 4. Tamper with the compiled-in official public key
    ├── 4.1 Modify source — requires repo write + code review bypass
    │   └── Mitigation: PR review, branch protection, `check_no_secrets.sh`
    └── 4.2 Modify on device — requires root, which is the thing we protect
        └── Mitigation: daemon integrity check on startup
```

### 5.2 Attacker goal: run arbitrary code with uid=0 via astrad

```
GOAL: arbitrary root command execution via astrad
├── 1. Reach astrad.sock
│   ├── 1.1 From a local unprivileged app
│   │   └── Mitigation: SELinux astrad domain; UDS perms 0660 astrad:astrad
│   └── 1.2 From a sandboxed module
│       └── Mitigation: sandbox has no astrad.sock visibility (mount ns)
├── 2. Authenticate as a trusted peer
│   └── Mitigation: peer auth requires app uid + signing cert match
├── 3. Issue a privileged command
│   ├── 3.1 With a forged capability token
│   │   └── Mitigation: tokens are HMACed with a daemon-only secret
│   └── 3.2 With a real but expired/revoked token
│       └── Mitigation: denylist + expiry check on every call (E-5)
└── 4. Exploit a daemon parsing bug
    ├── 4.1 In protobuf handler
    │   └── Mitigation: fuzz target `ipc/protobuf_handler`; seccomp on astrad
    └── 4.2 In module binary loader
        └── Mitigation: dlopen happens inside the sandbox, not astrad
```

### 5.3 Attacker goal: persist a backdoored AstraVeil update

```
GOAL: device installs a backdoored AstraVeil APK
├── 1. MITM the update channel
│   └── Mitigation: TLS pinning on GitHub + checksum from trusted channel
├── 2. Compromise the GitHub Release
│   └── Mitigation: 2FA on the release account; release notes carry SHA-256
├── 3. Sign a malicious APK with a cert the device trusts
│   └── Mitigation: device only trusts ITS OWN installed cert (UpdateVerifier)
└── 4. Trick the user into sideloading
    └── Mitigation: PackageInstaller gated behind UpdateVerifier; sideload
        path warns the user and refuses if signature mismatches
```

---

## 6. Residual risks

These are the risks we have consciously accepted after mitigation. Each one
is reviewed at every quarterly security review.

| ID | Residual risk | Why accepted | Monitoring |
|----|---------------|--------------|------------|
| RR-1 | A sufficiently resourced attacker with a 0-day in the Linux kernel can escape any userspace sandbox. | Defense against kernel 0-days is out of scope for any userspace root platform. | Watch upstream kernel CVE feeds; ship `AstraKernel` policy when available. |
| RR-2 | If the user's device is rooted by an unrelated tool, that tool's root can write `trusted_developers.json` directly. | We cannot defend against an attacker who already has root on the same device. | `AstraLogger` records all trust-store mutations; UI surfaces unexpected additions. |
| RR-3 | A user who blindly grants every permission request defeats least privilege. | Usability constraint; we cannot refuse the user's own choice. | UI shows cumulative risk; "are you sure" gates on risk ≥ 70. |
| RR-4 | The official Ed25519 private key, if leaked, allows OFFICIAL modules until the published public key is rotated. | Rotation requires a software update. | Public key is configurable via remote config (signed); rotation path is documented in `MODULE_DEVELOPER_GUIDE.md`. |
| RR-5 | A malicious module that exploits a bug in a `RootProvider` (Magisk/KernelSU/APatch) can reach uid 0 outside our sandbox. | We do not own those codebases. | `ProviderSelector` prefers the most-isolated available provider; `AstraRootProvider` is the long-term answer. |
| RR-6 | Side-channel attacks (timing, power) against Ed25519 verification are not explicitly mitigated in JVM `Signature`. | Constant-time Ed25519 implementations exist in native libs; the JVM provider is acceptable for the verification volumes involved. | Track JDK security notes; consider `:rust` Ed25519 verifier if volumes grow. |
| RR-7 | Audit log can be filled (DoS) by a misbehaving module causing many audit writes. | Log is ring-buffered; oldest entries age out. | Monitor audit log size; alert user if a single module exceeds 10% of buffer in 1h. |

---

## 7. Security testing requirements

Every control in §4 must have at least one automated test. The CI pipeline
must fail on any regression. Tests are organized by component.

### 7.1 Update verification (`:app`)

| Test | Asserts |
|------|---------|
| `UpdateVerifierTest. acceptsMatchingChecksumAndSignature` | A genuine APK passes both checks. |
| `UpdateVerifierTest. rejectsFlippedByte` | A single-byte change to the APK fails the checksum check. |
| `UpdateVerifierTest. rejectsApkWithWrongSigner` | An APK signed by a different cert fails signature check even if checksum is valid. |
| `UpdateVerifierTest. rejectsMissingFile` | A non-existent or zero-length APK returns `error != null` and `isInstallable == false`. |
| `UpdateVerifierTest. rejectsNullChecksum` | A null expected checksum fails verification. |
| `UpdateVerifierTest. logsRejectionReason` | On rejection, `AstraLogger.e` is called with a non-null reason. |

### 7.2 Module signature verification (`:modules`)

| Test | Asserts |
|------|---------|
| `ModuleSignatureVerifierTest. verifiesOfficiallySignedModule` | A `.avm` signed by the official key returns `OFFICIAL` + `VERIFIED`. |
| `ModuleSignatureVerifierTest. verifiesTrustedDeveloperModule` | A `.avm` signed by a key in `trustedKeys` returns `TRUSTED_DEVELOPER`. |
| `ModuleSignatureVerifierTest. acceptsUnknownDeveloper` | A validly-signed `.avm` whose key is neither official nor trusted returns `UNKNOWN_DEVELOPER` (still `VERIFIED`). |
| `ModuleSignatureVerifierTest. detectsTamperedEntry` | Modifying any entry inside the `.avm` after signing yields `INVALID` with reason `"content tampered: …"`. |
| `ModuleSignatureVerifierTest. rejectsStaleManifestOnNewPayload` | Replacing an entry but keeping the old manifest fails. |
| `ModuleSignatureVerifierTest. rejectsReplayedCertWithoutSig` | Copying only the `CERT` entry from a signed module into an unsigned one fails signature verification. |
| `ModuleSignatureVerifierTest. returnsUnsignedWhenMetaInfMissing` | A `.avm` with no `ASTRAVEIL.MF/SIG/CERT` returns `UNSIGNED`. |
| `ModuleSignatureVerifierTest. returnsInvalidOnCorruptSig` | Truncated `.SIG` returns `INVALID` with a `verification error: …` reason. |

### 7.3 Developer key store (`:modules`)

| Test | Asserts |
|------|---------|
| `DeveloperKeyStoreTest. persistsAcrossInstances` | `trust()` then a new `DeveloperKeyStore` reads it back. |
| `DeveloperKeyStoreTest. revokeRemovesKey` | `revoke()` removes the key from `trustedKeySet()`. |
| `DeveloperKeyStoreTest. ignoresCorruptFile` | A malformed `trusted_developers.json` does not throw; the store starts empty. |

### 7.4 Signing tool (`tools/avm-sign`)

| Test | Asserts |
|------|---------|
| `AvMSignerTest. keygenProducesUsableKeyPair` | `keygen` emits a valid Ed25519 keypair usable for `sign`. |
| `AvMSignerTest. signedArtifactVerifies` | A `.avm` produced by `AvMSigner sign` passes `ModuleSignatureVerifier.verify()`. |
| `AvMSignerTest. roundTripsAllEntries` | Every original entry is present in the signed artifact with identical bytes. |

### 7.5 Sandbox & permission enforcement (`:core`, `:modules`, `:daemon`)

| Test | Asserts |
|------|---------|
| `SandboxPolicyResolverTest. lowRiskGetsLockedProfile` | risk < 30 → no fs, no ns, no property. |
| `SandboxPolicyResolverTest. highRiskRequiresApproval` | risk ≥ 70 → profile is full but install requires user approval. |
| `PermissionEngineTest. dangerousPermissionsRequireApproval` | `mount`, `su`, `kernel_hook`, `namespace` cannot be silently granted. |
| `PermissionEngineEdgeCasesTest. revokedTokenIsRejectedOnNextCall` | A capability denied mid-session is honored on the next call. |
| `ModuleWatchdogTest. cpuHogIsKilled` | A module exceeding its CPU quota is terminated and audited. |
| `SandboxValidatorTest. pathTraversalRejected` | `..` in entry names fails validation. |

### 7.6 IPC & daemon (`:daemon`, C++)

| Test | Asserts |
|------|---------|
| `test_peer_auth.cpp` | Unauthenticated UDS peers are refused. |
| `test_frame_codec.cpp` | Malformed length prefixes are rejected without crashing. |
| `socket_server` integration | Per-peer rate limit disconnects abusive peers. |
| `audit_logger` integration | Every privileged op produces exactly one `AuditEntry`. |

### 7.7 Fuzzing & static analysis

- **libFuzzer** target on `protobuf_handler` — minimum 1M execs per PR.
- **AFL++** target on `AvmManifestParser` (zip parsing path).
- **clang-tidy** with `cert-*`, `bugprone-*`, `cppcoreguidelines-*` on `:daemon`.
- **detekt** + **Android Lint** on Kotlin; CI fails on any `Error` severity.
- **`scripts/check_no_secrets.sh`** scans for committed private keys/tokens; runs on every push.

### 7.8 Manual / red-team checklist (per release)

1. Attempt to install a `.avm` with a byte-flipped entry — must be rejected.
2. Attempt to install an unsigned `.avm` — must surface `UNSIGNED` and refuse
   install unless the user explicitly opts into unsigned modules (developer
   mode only).
3. Attempt to install an APK update signed by a different cert — must be
   refused with a clear user-facing reason.
4. Revoke a trusted-developer key, then re-run the module — must fail.
5. Fill the audit log past capacity — must age out gracefully, must not
   crash the daemon.
6. Kill `astrad` mid-operation — on restart, the in-flight audit entry
   must be finalized as `OUTCOME_CRASHED`.
7. Inspect `trusted_developers.json` permissions on a rooted device — must
   be `0600`, owner = app uid.

---

## Appendix A — Revision history

| Date | Change | Author |
|------|--------|--------|
| Initial | Document created (PR-D). | security-eng |
