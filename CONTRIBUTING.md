# Contributing to AstraVeil

Thanks for your interest in contributing to AstraVeil — the Android root
capability operating layer. This document covers the development workflow,
code standards, and module boundaries.

---

## Branch strategy

```
main          ← stable; only merged via PR with passing CI
├── feat/*    ← new features
├── fix/*     ← bug fixes
├── docs/*    ← documentation
├── refactor/* ← refactors (no behaviour change)
└── ci/*      ← build / CI changes
```

## Development workflow

1. **Fork** the repository and clone your fork.
2. **Branch**: `git checkout -b feat/my-feature`
3. **Code** + write/update tests.
4. **Verify locally** — all of the following must pass:
   ```bash
   # Kotlin (app + libraries)
   ./gradlew testDebugUnitTest detekt lintDebug assembleDebug

   # Coverage (opt-in, non-blocking)
   ./gradlew jacocoTestReport

   # Rust policy crate
   cd rust && cargo clippy --all-targets -- -D warnings && cargo test

   # Daemon (C++20) — requires a host or NDK toolchain
   cmake -S daemon -B daemon/build && cmake --build daemon/build
   ctest --test-dir daemon/build

   # AstraHub index
   python3 tools/validate-hub.py --all --strict
   ```
5. **Commit** using [Conventional Commits](https://www.conventionalcommits.org/):
   ```
   feat(core): add capability lease expiry notification
   fix(providers): correct KernelSU detection on Android 15
   docs(arch): update module dependency graph
   ```
6. **Push** and open a Pull Request against `main`. Fill in the PR template.
7. Wait for **CI to pass** (Android, Rust, Code Quality, AstraHub validation)
   and address review feedback.

## Code standards

| Language   | Style / Linter | Config |
|------------|----------------|--------|
| Kotlin     | [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html) + detekt | `config/detekt/detekt.yml` |
| C++        | C++20, clang-tidy | `daemon/.clang-tidy` |
| Rust       | `rustfmt` + `clippy` | — |
| Protobuf   | [Google Proto Style Guide](https://protobuf.dev/programming-guides/style/) | — |
| All files  | `.editorconfig` | `.editorconfig` |

### Key rules
- **No hardcoded dependency strings** — use the Gradle version catalog
  (`gradle/libs.versions.toml`).
- **No wildcard imports** (except where detekt allows).
- **TODOs must reference an issue**: `TODO(#123)`, not bare `TODO`.
- **New public API** must have KDoc / Doxygen documentation.
- **IPC protocol changes** must update `proto/` definitions.
- **Security-relevant changes** must update `docs/THREAT_MODEL.md`.

## Module boundaries

Before modifying code, read these to understand what each module may and may
not depend on:

| Document | Content |
|----------|---------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | System architecture, Mermaid dependency graph |
| [docs/MODULE_CONTRACTS.md](docs/MODULE_CONTRACTS.md) | Inter-module API contracts, prohibitions |
| [docs/MODULE_DEPENDENCY_GRAPH.md](docs/MODULE_DEPENDENCY_GRAPH.md) | Gradle module DAG, dependency-direction rules |
| [docs/DATA_FLOW.md](docs/DATA_FLOW.md) | Data-flow sequence diagrams, IPC protocol |

**Golden rule**: dependencies only flow downward (`:app → :core → :proto`).
Never introduce an upward or circular dependency.

## Reporting security vulnerabilities

**Do not open a public issue for security vulnerabilities.**

Please report them privately via
[GitHub Security Advisory](../../security/advisories/new). See
[docs/SECURITY.md](docs/SECURITY.md) for the full policy.

## License

By contributing, you agree that your contributions will be licensed under the
project's [Proprietary License](LICENSE). Third-party `.avm` modules developed
against the SDK remain the property of their authors.
