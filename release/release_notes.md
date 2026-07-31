# AstraVeil v1.1.2 — Release Notes

## Overview

Notification integration + Dashboard RootAccessCard + Phase 0 honesty.

## What's new in v1.1.2

- **Notification Integration** — AstraNotificationManager now called at 5 points:
  module install/uninstall, daemon connect/disconnect, update available.
  Users receive actual notifications for these events.
- **Dashboard RootAccessCard** — New card on Dashboard showing root access
  grant flow: idle (Grant Root Access button) → requesting (spinner + hint
  about system dialog) → granted (green) / denied (amber + retry) / no
  backend (info).
- **Module RUNNING Phase 0 Honesty** — Module cards now show amber warning
  when state is RUNNING: "Loaded in app process · Phase 0 (not isolated,
  not rooted)". Users are no longer misled into thinking modules run in
  isolated root processes.

## Full feature set

See v1.1.1 release notes.
