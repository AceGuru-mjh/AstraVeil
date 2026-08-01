# AstraVeil v1.7.0 — Release Notes

## Overview

**P2-17 closed**: the four-way container split (GlassCard / LiquidGlassCard /
GlassSurface / bare M3 Card) is replaced by a single `AstraSurface` /
`AstraCard` primitive carrying a declared `SurfaceTier`. Liquid glass is no
longer decorative — it is reserved exclusively for live state, so it carries
signal instead of noise.

## What's new in v1.7.0

### Unified Surface API
- `SurfaceTier` enum: `CONTENT` (quiet, readable, cheap) / `ELEVATED` (slight
  emphasis) / `LIQUID` (alive — full refraction + specular + aberration).
- `AstraSurface` / `AstraCard`: the single container primitive. The caller
  declares *what kind* of surface it wants via `tier`; the platform picks the
  rendering. No more guessing which of four containers to use.
- `AstraShapes`: canonical corner-radius scale (sm=10 / md=16 / lg=22 / xl=28
  / pill). `GlassShapes` kept as a `@Deprecated` alias.

### Liquid = "alive" (design principle)
Liquid glass now carries meaning. It is reserved exclusively for:
- **Running module card** — LIQUID + AccentTeal when `state == RUNNING`
- **ADB console** — always LIQUID + AccentViolet (live terminal)
- **Download / install in progress** — LIQUID + AccentViolet
- **Active provider card** — LIQUID + AccentViolet when active
- **Hub module downloading** — LIQUID + AccentViolet when `downloadingId` matches

Static content (settings, policy cards, audit rows, diagnostics, about) uses
the quiet CONTENT tier. Key summaries use ELEVATED. Aim for ≤ 6 liquid
surfaces on screen at once — worst case currently is 2 (UpdateCenter during
download).

### Deprecated shims
- `GlassCard` → `AstraCard(tier = CONTENT)`
- `LiquidGlassCard` → `AstraCard(tier = LIQUID)`
- `GlassSurface` → `AstraSurface(tier = CONTENT)`

No real callers existed; the shims are retained for source compatibility and
will be removed in a future release.

### Bare M3 Card cleanup
4 bare `Card(` usages migrated to `AstraCard(CONTENT)`:
DiagnosticsScreen (console report), SecurityReviewDialog, SuRequestDialog,
and ProviderScreen static info cards.

## Verification
- CI: all 5 checks green on PR #83 (build-and-test, daemon-build-and-test,
  rust, rust-fuzz, cpp-fuzz).
- 0 leftover bare `Card()` in migrated files.
- 0 `AstraCard(containerColor=...)` usages (removed param — no caller used it).
- Max LIQUID on any screen = 2 (well under the ≤6 budget).
- All pre-existing `AstraCard` callers compile unchanged (modifier +
  contentPadding + trailing lambda are all compatible with the new signature).

## Audit status
P2-17 closed. Remaining P2 item: P2-20 (multi-generation model convergence —
ModuleManifest / ModuleState / ModuleRuntime etc.); the audit recommends
this after P0/P1, which are now complete.
