# AstraVeil v1.0.5 — Release Notes

## Overview

Patch release: DiagnosticsScreen recommendations now dynamic.

## What's new in v1.0.5

- **Dynamic Diagnostics Recommendations** — Replaced hardcoded recommendations
  with ones derived from real device state:
  - Provider: "No Root Backend" (amber) vs "<provider> Active" (green)
  - SELinux: "SELinux Enforcing" shown only when status is ENFORCING
  - OverlayFS: "OverlayFS Unavailable" shown only when not supported
  - Manufacturer: Samsung Knox / Xiaomi HyperOS tips shown conditionally
- **AstraCard** — DiagnosticsScreen now uses AstraCard for visual consistency

## Full feature set

See v1.0.0 release notes for the complete feature list.
