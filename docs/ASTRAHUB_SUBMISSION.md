# Submitting a Module to AstraHub

AstraHub is the curated module index. Listing is gated on signature
verification and capability declaration.

## Requirements
1. Your module MUST be signed (see MODULE_DEVELOPER_GUIDE.md §6).
2. `module.json` MUST declare `requiredCapabilities` honestly.
3. Your signing key fingerprint will be shown to users.

## Steps
1. Build and sign your `.avm`.
2. Compute its SHA-256: `sha256sum module.avm`.
3. Host the `.avm` at a stable URL (GitHub Releases recommended).
4. Fork this repo and add an entry to `astrahub/modules/index.json`:

```json
{
  "id": "com.yourname.yourmod",
  "name": "Your Module",
  "version": "1.0.0",
  "apiVersion": 3,
  "author": "Your Name",
  "description": "What it does.",
  "requiredCapabilities": ["overlayfs"],
  "optionalCapabilities": [],
  "trustLevel": "UNKNOWN_DEVELOPER",
  "signatureFingerprint": "<your key fingerprint>",
  "downloadUrl": "https://.../yourmod-1.0.0.avm",
  "sha256": "<sha256 of the .avm>",
  "sizeBytes": 12345,
  "publishedAt": "2026-07-31T00:00:00Z",
  "minAstraVeilVersion": "1.1.0"
}
```

5. Open a PR. CI will:
   - Download your `.avm` and verify the SHA-256 matches
   - Verify the signature is well-formed
   - Validate the manifest schema and capability vocabulary
6. After merge, your module appears in AstraHub. Users see your
   fingerprint and must trust your key once (TRUSTED_DEVELOPER), or
   approve per-install (UNKNOWN_DEVELOPER).

## Trust levels in AstraHub
| trustLevel | Meaning |
|---|---|
| OFFICIAL | Signed by the AstraVeil release key (team-reviewed) |
| TRUSTED_DEVELOPER | Signed by a key the user has trusted |
| UNKNOWN_DEVELOPER | Valid signature, user must approve |

## Removal
Modules violating policy (malware, undeclared capabilities, broken
signatures) are removed from the index. Removal from the index does not
uninstall already-installed copies, but blocks new installs/updates.
