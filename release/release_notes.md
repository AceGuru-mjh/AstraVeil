# AstraVeil v1.9.0 — Release Notes

## Overview

Three fixes in one release: **settings integration** (all 12 entries now
open real functional screens), **liquid glass cleanup** (all liquid glass
removed, pure Material 3), and **capability compatibility UI** (shown in
the pre-install security review dialog).

## What's new in v1.9.0

### Settings integration (root cause fix)
All 9 settings sub-routes previously led to `ComingSoonScreen`. Now every
entry opens a real functional screen:

| Entry | Screen | Wired to |
|---|---|---|
| Security | SecuritySettingsScreen | AstraConfig.dangerousApproval, DeveloperKeyStore, CommandAuditLogger |
| Provider | ProviderSettingsScreen | ProviderRegistry.all() + detectActive() |
| Daemon | DaemonSettingsScreen | AstraConfig.daemonEnabled |
| Modules | ModulesSettingsScreen | AstraConfig.moduleAutoStart |
| Appearance | AppearanceScreen | SharedPreferences (theme_mode) |
| Language | LanguageScreen | AstraStrings.setLocaleOverride + Activity.recreate() |
| Notifications | NotificationsScreen | SharedPreferences (5 channel toggles) |
| Backup | BackupSettingsScreen | config.load(), DeveloperKeyStore, CommandAuditLogger.export() |
| Developer | DeveloperSettingsScreen | AstraConfig.logLevel + AstraLogger.setMinLevel |
| Diagnostics | DiagnosticsScreen | (already existed) |
| Updates | UpdateCenterScreen | (already existed) |
| About | AboutScreen | (already existed) |

- 12 NavHost routes registered in AstraVeilApp.kt (0 ComingSoonScreen
  placeholders remain)
- 12 SettingsScreen entries with matching routes
- Notifications: 5 channel toggles persisted via SharedPreferences
- Language: selection persisted + Activity.recreate() for immediate effect

### Liquid glass cleanup (all removed)
The liquid glass rendering (refraction + specular + chromatic aberration)
has been completely removed. Pure Material 3 throughout.

- `AstraCard.kt`: LIQUID tier now renders as ELEVATED with accent tint
  (no LiquidGlassSurface). `SurfaceTier` enum preserved for source
  compatibility — all `tier = SurfaceTier.LIQUID` calls still compile
  and work, they just get an elevated card instead of liquid glass.
- Replaced `LiquidGlass.AccentTeal` → `AstraTeal.copy(alpha=0.08f)` and
  `LiquidGlass.AccentViolet` → `AstraAccent.copy(alpha=0.08f)` in 5 files
- Deleted 9 dead design files: LiquidGlassSurface, LiquidGlass,
  LiquidGlassNavigationBar, LiquidGlassDivider, LiquidGlassCard, GlassCard,
  GlassSurface, AstraGlass, AstraElevation
- Design system now: **2 files** (AstraCard.kt + AstraShapes.kt) — pure M3

### Capability compatibility UI
`SecurityReviewDialog` now accepts an optional `CompatibilityReport`
parameter. When provided, a compatibility section appears in the pre-install
dialog showing:
- ✓ "Compatible with this device" (green) when all required capabilities
  are satisfied
- ⚠ "Missing required capabilities" (amber) with a list of what's missing
  when the device cannot support the module

## Verification
- CI: all 5 checks green on PR #87 (build-and-test, daemon-build-and-test,
  rust, rust-fuzz, cpp-fuzz)
- 0 remaining `LiquidGlass` references in app code
- 0 remaining `ComingSoonScreen` usages in NavHost routes
- 12/12 settings entries map to 12/12 NavHost routes
