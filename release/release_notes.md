# AstraVeil v1.2.0 — Release Notes

## Overview

Major feature: ADB Shell mode with real uid 2000 execution.

## What's new in v1.2.0

- **ADB Shell Mode** — Terminal now has three modes: ROOT (su -c),
  ADB (su 2000 sh -c — true uid 2000 shell user simulation), and
  SHELL (app's own UID). ADB mode provides authentic `adb shell`
  experience on-device.
- **AdbManager** — Real ADB status detection: enabled, daemon running,
  root mode, TCP/IP port. Build uid 2000 commands via `su 2000 sh -c`.
- **Mode-Aware Quick Commands** — ADB mode shows ADB-specific commands
  (getprop, pm list packages, dumpsys battery, wm size, logcat, etc).
- **Mode Cycle Button** — Color-coded button (green ROOT / amber ADB /
  gray SHELL) replaces the old Switch toggle.

## Full feature set

See v1.1.2 release notes.
