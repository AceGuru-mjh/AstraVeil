# AstraVeil v1.0.2 — Release Notes

## Overview

UI reverted to standard Material 3. Replaced all Liquid Glass custom
rendering with plain M3 Card + NavigationBar for reliability and
clarity.

## What's new in v1.0.2

- **Standard M3 Navigation Bar** — Replaced custom Liquid Glass nav bar
  with Material 3 NavigationBar (surface color + tonal elevation).
  Automatically handles system insets. Selected item uses primary color.
- **AstraCard** — New M3 Card wrapper replacing GlassCard/LiquidGlassCard.
  Plain surfaceVariant background + 1dp elevation. No blur, no specular,
  no custom rendering.
- **Removed FLAG_BLUR_BEHIND** — No longer needed without Liquid Glass.
- **All screens migrated** — Dashboard, Provider, Superuser, Diagnostics,
  About, Settings, Updates, Modules, SecurityReviewDialog, SuRequestDialog
  all use AstraCard and M3 color tokens.

## Full feature set

See v1.0.0 release notes for the complete feature list.
