# AstraVeil v1.0.3 — Release Notes

## Overview

Patch release: replaced hardcoded app list with real PackageManager query.

## What's new in v1.0.3

- **Real App List** — Superuser screen now shows actual installed user apps
  from PackageManager (was hardcoded: Termux, ADB Shell, Magisk, etc.).
  Uses ApplicationInfoFlags.of on API 33+, deprecated overload below.
  Su grants checked via PermissionEngine.

## Full feature set

See v1.0.0 release notes for the complete feature list.
