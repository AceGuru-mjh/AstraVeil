# AstraVeil v2.0.0 — Release Notes

## Overview

Settings consolidation: 12 entries → 10. Two merged screens replace four
separate ones, and the theme preference now actually drives the color scheme.

## What's new in v2.0.0

### Preferences (Appearance + Language merged)
- **Theme mode** (Dark / Light / System): persisted to
  `SharedPreferences("astra_ui_prefs")/"theme_mode"`. `MainActivity` reads
  this at startup and passes a `ThemeMode` to `AstraVeilTheme` — the
  preference now actually controls the color scheme (previously it was
  stored but never read by the theme).
- **Language** (English / 中文): persisted + `AstraStrings.setLocaleOverride`
  + `Activity.recreate()` — applies immediately.
- Both settings call `recreate()` so the user sees the change instantly.

### Update & Backup (Update Center + Backup merged)
- **Update card**: `when`-matches the `UpdateState` sealed class
  (Idle / Checking / Available / Downloading / Verifying / Installing /
  Success / Latest / Error) and renders the appropriate UI — check
  button, download progress bar, verify spinner, install button, error
  message. Wired to the real `UpdateViewModel` (`checkForUpdate()` /
  `downloadAndInstall()`).
- **Backup card**: export/import a ZIP via SAF (CreateDocument /
  OpenDocument) containing the module registry, trusted developer keys,
  privileged command audit log, and a backup meta marker. No storage
  permission needed.

### Theme.kt
- Added `ThemeMode` enum (DARK / LIGHT / SYSTEM) with `fromString()` /
  `toPrefString()`.
- Added `AstraVeilTheme(themeMode: ThemeMode, content)` overload that
  delegates to the existing elaborate color schemes — the dark-first
  violet/teal palette and status bar setup are preserved.

### Deleted (4 files → 2 merged screens)
- `AppearanceScreen.kt` → merged into `PreferencesScreen.kt`
- `LanguageScreen.kt` → merged into `PreferencesScreen.kt`
- `UpdateCenterScreen.kt` → merged into `UpdateBackupScreen.kt`
- `BackupSettingsScreen.kt` → merged into `UpdateBackupScreen.kt`

### Settings entries: 12 → 10
```
Security · Provider · Modules · Daemon · Notifications · Preferences ·
Update & Backup · Diagnostics · Developer · About
```
All 10 entries map to 10 NavHost routes (0 ComingSoonScreen placeholders).

## Verification
- CI: all 5 checks green on PR #89.
- 10/10 entry routes ↔ 10/10 composable routes.
- Theme preference read by MainActivity → drives AstraVeilTheme.
- UpdateBackupScreen wired to real UpdateViewModel sealed-class state.
