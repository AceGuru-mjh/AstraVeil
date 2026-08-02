# AstraHub Module Index Schema

## Schema Version

Current: `1`

The `schemaVersion` field is a monotonically increasing integer. When the
schema changes in a backwards-incompatible way, increment this number.
AstraHub clients MUST reject indices with a `schemaVersion` they don't support.

## Top-level Structure

```json
{
  "schemaVersion": 1,
  "updatedAt": "2026-07-31T00:00:00Z",
  "modules": [ HubModule, ... ]
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `schemaVersion` | integer | ✅ | Schema version (currently 1) |
| `updatedAt` | string (ISO 8601) | ✅ | When the index was last regenerated |
| `modules` | array&lt;HubModule&gt; | ✅ | List of available modules |

## HubModule

```json
{
  "id": "com.astraveil.fontmod",
  "name": "Font Replacer",
  "version": "1.2.0",
  "apiVersion": 3,
  "author": "AstraVeil Team",
  "description": "Systemless font replacement via overlayfs.",
  "requiredCapabilities": ["overlayfs", "mount_namespace"],
  "optionalCapabilities": [],
  "trustLevel": "OFFICIAL",
  "signatureFingerprint": "ab:cd:ef:...",
  "downloadUrl": "https://...",
  "sha256": "e3b0c442...",
  "sizeBytes": 184320,
  "publishedAt": "2026-07-30T12:00:00Z",
  "minAstraVeilVersion": "1.1.0"
}
```

| Field | Type | Required | Validation | Description |
|-------|------|----------|------------|-------------|
| `id` | string | ✅ | `^[a-zA-Z0-9][a-zA-Z0-9._-]{0,127}$` | Unique module identifier (dotted notation) |
| `name` | string | ✅ | non-empty | Human-readable display name |
| `version` | string | ✅ | semver | Module version |
| `apiVersion` | integer | ✅ | 1, 2, or 3 | AstraVeil Module API version |
| `author` | string | ❌ | — | Author name |
| `description` | string | ❌ | max 500 chars | Short description |
| `requiredCapabilities` | array&lt;string&gt; | ✅ | from vocabulary | Capabilities the device MUST have |
| `optionalCapabilities` | array&lt;string&gt; | ❌ | from vocabulary | Capabilities that enhance the module |
| `trustLevel` | string | ✅ | `OFFICIAL` \| `TRUSTED_DEVELOPER` \| `UNKNOWN_DEVELOPER` | Signing trust level |
| `signatureFingerprint` | string | ✅ | `^([0-9a-f]{2}:){31}[0-9a-f]{2}$` | SHA-256 of signing cert (colon-separated) |
| `downloadUrl` | string | ✅ | https:// URL | Stable download URL for `.avm` file |
| `sha256` | string | ✅ | `^[0-9a-f]{64}$` | SHA-256 of the `.avm` file (transport integrity) |
| `sizeBytes` | integer | ✅ | > 0 | File size in bytes |
| `publishedAt` | string | ✅ | ISO 8601 | When this version was published |
| `minAstraVeilVersion` | string | ❌ | semver | Minimum AstraVeil version required |

## Capability Vocabulary

The following capability names are valid in `requiredCapabilities` and
`optionalCapabilities`. They MUST match the daemon's `probe_detector.cpp`
output keys exactly:

| Capability | Description |
|------------|-------------|
| `root` | Root execution (uid=0) |
| `overlayfs` | OverlayFS filesystem support |
| `mount_namespace` | Mount namespace isolation |
| `pid_namespace` | PID namespace isolation |
| `net_namespace` | Network namespace isolation |
| `selinux` | SELinux is present and controllable |
| `system_write` | /system is mounted read-write |
| `zygisk` | Zygisk framework available |
| `boot_patch` | Boot partition can be patched |

## Trust Levels

| Level | Meaning | Signature Required |
|-------|---------|-------------------|
| `OFFICIAL` | Signed by AstraVeil release key (team-reviewed) | ✅ Ed25519 |
| `TRUSTED_DEVELOPER` | Signed by a key the user has trusted | ✅ Ed25519 |
| `UNKNOWN_DEVELOPER` | Valid signature, user must approve per-install | ✅ Ed25519 |

Modules without a valid signature are NOT listed in AstraHub.

## Security Model

1. **Transport integrity**: `sha256` is verified against the downloaded file
   BEFORE handing off to the installer (defense in depth layer 1).
2. **Authorship + trust chain**: `signatureFingerprint` is verified by
   `ModuleSignatureVerifier` during install (defense in depth layer 2).
3. **Capability compatibility**: `requiredCapabilities` are checked against
   the device's capability matrix by `CapabilityCompatibilityChecker`
   before install is allowed.
4. **TrustGate**: `TrustGate.requireInstallable(strict = true)` enforces
   that only verified signatures proceed to installation.

## CI Validation (for PRs adding modules)

CI MUST verify:
1. `sha256` matches the actual file at `downloadUrl`
2. `signatureFingerprint` is a valid SHA-256 fingerprint format
3. `requiredCapabilities` only contains vocabulary terms
4. `id` matches the module's internal `module.json` id
5. `version` matches the module's internal `module.json` version
