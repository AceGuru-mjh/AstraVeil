# AstraVeil v1.0.1 — Release Notes

## Overview

Patch release with comprehensive UI overhaul: Liquid Glass navigation bar,
real Superuser app management, multi-probe root verification, and permission
declarations.

## What's new in v1.0.1

- **Liquid Glass Navigation Bar** — Floating pill-shaped bottom navigation
  with multi-layer optical rendering (base tint + gradient + specular
  highlight + border glow). Replaces the standard Material3 NavigationBar.
- **Real Superuser Screen** — Queries PackageManager for actual installed
  user apps. Each app shows its real icon, name, and package name. su grants
  are tracked via PermissionEngine (persisted to astra_config.json). No more
  hardcoded "示例应用".
- **Multi-Probe Root Test** — `testRootCapability()` now runs 6 read-only
  probes (id, getenforce, uname -r, ls /data/adb/, which su, cat /proc/mounts)
  instead of just `id`. Each probe shows ✅/❌ with output.
- **Permission Declarations** — Added QUERY_ALL_PACKAGES for app list queries.
  POST_NOTIFICATIONS requested at runtime on Android 13+.
- **Liquid Glass Surface Fix** — Removed incorrect Modifier.blur() that was
  blurring the glass's own content instead of the background.
- **AppIcon without Accompanist** — Drawable → Bitmap → ImageBitmap conversion
  via remember cache. No third-party dependency.

## Full feature set (from v1.0.0)

- Liquid Glass Design System (4-layer optical rendering)
- AstraUI — Compose dashboard with Chinese localization (i18n)
- Module Trust Pipeline — SHA-256 + manifest pre-parse + risk analysis
- AVM Module Runtime — install/uninstall/start/stop with real System.load + dlopen
- AstraDaemon — C++20 system service with Unix socket IPC
- Module Runner — fork + sandbox (namespace + seccomp 40+ + Landlock) + dlopen
- Rust Security — PolicyEngine + SandboxPolicy + SHA-256 attestation
- Provider Runtime — Magisk / KernelSU / APatch detection + intelligent routing
- DaemonManager — App-side daemon connection with retry
- Magisk Module Package — service.sh + init.rc for deploying astrad
- 126 Unit Tests + Instrumented Test scaffold

## Minimum requirements

- Android 8.0 (API 26) or later
- ARM64 device
- Java 17 or later for building

## Install

1. Install the APK: `adb install app-release.apk`
2. For daemon support: flash the Magisk module zip from releases
