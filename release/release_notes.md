# AstraVeil v1.0.3 — Release Notes

## Overview

Visual overhaul: Liquid Glass rendering ported from liquid-glass-react.
10-layer optical renderer with edge refraction, chromatic aberration,
specular highlights, elastic press, and window-level background blur.

## What's new in v1.0.3

- **Liquid Glass React Port** — 10-layer renderer adapted from the
  liquid-glass-react web library:
  - Edge refraction: 12 concentric strokes with quadratic alpha falloff
    (simulates SVG feDisplacementMap SDF)
  - Chromatic aberration: R/B channel split at edges (±1.5px offset)
  - Specular highlight: radial gradient follows touch, intensifies on press
  - Multi-layer border: inner glow + outer + overlay (mix-blend simulation)
  - Elastic press: spring(dampingRatio=0.65) → liquid wobble overshoot
  - Animated shadow: 12dp → 4dp on press
- **Window-Level Background Blur** — FLAG_BLUR_BEHIND (API 31+) with
  radius=48. Real frosted-glass background behind all surfaces.
- **Nav Bar Fixes**:
  - windowInsetsPadding(navigationBars) — no longer hidden behind gesture bar
  - 14dp vertical spacing (was 10dp)
  - 28dp corner radius (was 26dp)
  - NavLabelActive/Inactive tokens (not hardcoded alpha)
- **No Modifier.blur()** — removed the bug that blurred the glass's own
  content into an amorphous blob. Background blur is now window-level.

## Full feature set (from v1.0.0+)

- Liquid Glass Design System (10-layer renderer + window blur)
- Real Superuser (Magisk DB read/write + request logs)
- Multi-Probe Root Test (6 probes)
- Module Trust Pipeline (SHA-256 + manifest + risk + signature)
- AVM Module Runtime (install/uninstall/start/stop with System.load + dlopen)
- AstraDaemon (C++20 Unix socket IPC)
- Module Runner (fork + sandbox + dlopen)
- Rust Security (PolicyEngine + SandboxPolicy + SHA-256)
- Provider Runtime (Magisk/KernelSU/APatch detection + routing)
- DaemonManager + Magisk Module deployment package
- 126 Unit Tests + Instrumented Test scaffold
- Chinese localization (i18n)

## Minimum requirements

- Android 8.0 (API 26) or later
- ARM64 device
- Magisk (for su policy management), KernelSU, or APatch for root features

## Install

1. Install the APK: `adb install app-release.apk`
2. For daemon support: flash the Magisk module zip from releases
