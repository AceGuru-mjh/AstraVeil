# AstraVeil v1.2.1 — Release Notes

## Overview

Superuser page now has real, usable tools instead of placeholders.

## What's new in v1.2.1

- **Terminal Launcher Card** — Clickable card on Superuser page that
  opens the full-screen terminal. Shows three mode badges (ROOT/ADB/SHELL)
  so users know what they're opening. This is an action (open), not a
  toggle (enable/disable).
- **Embedded ADB Console** — Real ADB console directly on the Superuser
  page. Live status dots (ADB enabled / adbd running / root available /
  USB-TCP). Dark console output area. Quick command chips (getprop, pm
  list packages, dumpsys battery, wm size, etc). Input field + execute
  button. Commands run as uid 2000 with root, or app UID without.
- **No Toggles** — Every interaction produces real output. No more
  switches that do nothing.

## Full feature set

See v1.2.0 release notes.
