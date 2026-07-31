# AstraVeil v1.3.0 — Release Notes

## Overview

Major UI overhaul: Dashboard and Superuser screens completely rewritten.

## What's new in v1.3.0

### Dashboard
- **Device header** — Shows manufacturer, model, Android version, API level
- **Quick actions** — Terminal / Superuser / Modules / Settings buttons
- **Daemon status** — Real-time AstraDaemon connection indicator
- **Grouped capabilities** — Human-readable labels, organized by category
- **Refresh button** — Re-probe device capabilities on demand
- **Root test** — Disabled when no provider, shows 6 probe results

### Superuser
- **App picker** — Add su policy for any installed app
- **Search/filter** — Find policies by package name or UID
- **App icons** — Real icons from PackageManager
- **Logging/notification toggles** — Control Magisk DB fields per app
- **Su usage stats** — "used N×" aggregated from logs
- **ADB Shell entry** — uid 2000 special policy card
- **Refresh button** — Reload policies and logs
- **Terminal + ADB console** — Actually visible in UI (were imported but not rendered)

## Full feature set

See v1.2.1 release notes.
