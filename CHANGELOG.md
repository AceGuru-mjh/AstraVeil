# Changelog

All notable changes to AstraVeil are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Code quality toolchain: detekt, `.editorconfig`, `daemon/.clang-tidy`
- `.github/workflows/code-quality.yml` (detekt + clippy + editorconfig + clang-tidy)
- `.github/dependabot.yml` (Gradle, GitHub Actions, Cargo dependency updates)
- Conservative JaCoCo coverage reporting (`./gradlew jacocoTestReport`)
- Daemon security hardening compile/link flags (PIE, full RELRO, stack-protector,
  FORTIFY_SOURCE, non-executable stack)
- Governance: `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, PR + Issue templates
- `native/RUST_BUILD.md` — Rust build integration guide
- Canonical `ARCHITECTURE.md` with Mermaid dependency graph + build reference
- `daemon/proto/CMakeLists.txt` + `ASTRA_USE_PROTOBUF` option (JSON fallback default)
- `.github/workflows/hub-validate.yml` + `tools/validate-hub.sh`
- Completed `tools/validate-hub.py` (schema + semantic + format validation)

### Changed
- `core/build.gradle.kts`, `sdk/build.gradle.kts`: wire `api(project(":proto"))`
- `README.md`: documentation section expanded to a 12-row navigation table
- `docs/ARCHITECTURE.md` reduced to a redirect stub (canonical doc now at repo root)
- `:proto` module registered in `settings.gradle.kts` (was dead code)
- All `astrahub/*.json` canonicalized to 2-space indent + trailing newline

### Fixed
- Eliminated last hardcoded dependency (`kotlin-test` in `core/build.gradle.kts`)
- `tools/validate-hub.py` was a truncated stub (`def ok(msg)` with no body)

## [2.2.0] - 2026-08

### Added
- Superuser dashboard: lockdown, risk-based grouping, temporary grants
- Daemon real probes (`/proc`, `/sys`, `Build`) replacing prototype stubs
- Persistent terminal (ROOT / ADB / SHELL modes, cd/export persist)
- Root Chain Self-Test (7-link diagnostic)
- AstraHub module browsing + download + SHA-256 verification
- Module install (TrustGate + Zip Slip + TOCTOU protection)
- Update system (check / download / verify signature / install)
- Backup/restore (export/import registry + keys + audit log via SAF)

### Changed
- Settings consolidation (12 → 10 screens); theme preference now persists
- Liquid glass UI cleanup

## [2.1.0] - 2026-07

### Added
- Persistent terminal
- Root Chain Self-Test
- Diagnostics with provenance (P2-18)

## [2.0.0] - 2026-07

### Changed
- Settings consolidation (12 → 10)
- Theme preference actually works

## [1.9.0] - 2026-06

### Added
- Settings wiring (9 real screens)
- Capability UI

## [1.8.0] - 2026-06

### Added
- Signed APK pipeline (base64 keystore secret + verify)
