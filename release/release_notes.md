# AstraVeil v1.0.4 — Release Notes

## Overview

Critical fix: Superuser page now works on root devices.

## What's new in v1.0.4

- **magisk --sqlite** — Replaced `sqlite3` (not shipped by Magisk) with
  `magisk --sqlite` for all su policy database operations. This is the
  canonical Magisk interface for querying/modifying the su policy DB.
  The Superuser page was completely non-functional on root devices
  because `sqlite3: not found`.
- **Output format parser** — `magisk --sqlite` outputs rows as
  `key=value | key=value | ...` (not pipe-delimited values). Replaced
  the old positional parser with a key-value map parser.

## Root user path (now works)

```
Detect Magisk → magisk --sqlite SELECT → real policy list
→ user changes Allow/Ask/Deny → magisk --sqlite INSERT → Magisk DB updated
→ Termux su → Magisk checks DB → uses AstraVeil's policy
```

## Full feature set

See v1.0.0 release notes for the complete feature list.
