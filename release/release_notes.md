# AstraVeil v2.2.0 — Release Notes

## Overview

Superuser page upgraded from a static switch list to an **active root dashboard**.
Policy cards now show live countdowns, activity dots, and expandable details.
A panic lockdown button and risk-based grouping make the page a real security
control center.

## What's new in v2.2.0

### MagiskSuRepository — new real DB operations
- `grantTemporary(uid, pkg, minutes)`: uses Magisk's native `until` column —
  Magisk itself enforces expiry, so access auto-revokes even if AstraVeil is killed.
- `makePermanent(uid, pkg)`: clears the `until` field.
- `revoke(uid, pkg)`: immediate DENY.
- `lockdown()`: set ALL non-DENY policies to DENY, returns a snapshot for restore.
- `restorePolicies(snapshot)`: undo lockdown.

### SuPolicyCard — multifunction (was: static switches)
- **Status color bar**: green=allow, teal=temporary, amber=ask, red=deny.
- **Live countdown chip**: for temporary grants, updates every second.
- **Activity dots**: last 5 su requests (green=allowed, red=denied).
- **Stats line**: "today N · Xm ago".
- **Segmented control** with **long-press Allow = duration picker** (5min/15min/1h/8h/permanent).
- **Expandable details**: log/notify toggles + recent request history + action chips
  (Make Permanent / Revoke Now / Delete).

### LockdownBar — panic lockdown
- Red "Lock All" button with confirmation dialog (anti-mistouch).
- After lockdown: red banner showing count + Restore / Keep buttons.
- Restore undoes lockdown using the pre-lockdown snapshot.

### SuRiskGrouper — dynamic risk-based grouping
- `attentionScore` (0-100): recency (0-40) + today frequency (0-30) + policy risk (0-30).
- Groups: **Needs Attention** (≥50) / **Normal** (20-49) / **Dormant** (<20).
- Dynamic — frequent root users automatically float to the top.

### SuUsageStats enhanced
- Added `todayCount`, `lastUsed`, `recent` (last 5 log entries).
- `SuStatsAggregator.aggregate()` groups logs by uid with today filter.
- `relativeTime()` + `formatCountdown()` helpers.

### MatrixScorer
- Confirmed **does not exist** in the codebase — nothing to delete. The spec's
  deletion instructions were moot.

## Verification
- CI: all 5 checks green on PR #93.
- All new MagiskSuRepository methods write the real Magisk DB via `magisk --sqlite`.
- Policy card countdown updates every second via `LaunchedEffect`.
- Risk grouping recalculates on every policy/log refresh.

## Honest boundaries
- Real-time su interception and command-level audit still need the daemon (Phase 1).
- The `SuRequestDialog` exists but cannot be triggered yet — it waits for the daemon
  to listen on magiskd and forward requests to the app.
