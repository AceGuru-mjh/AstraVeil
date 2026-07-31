# AstraVeil v1.0.1 — Release Notes

## Overview

Patch release: removed unused RootManagerScreen with fake su switches.

## What's new in v1.0.1

- **Removed RootManagerScreen** — The old Superuser screen wrote su grants
  to AstraVeil's local PermissionEngine (DataStore), not Magisk's su
  database. This had no effect on actual su behavior. Replaced by
  SuperuserScreen (PR #48) which reads/writes Magisk's real
  /data/adb/magisk.db via su -c sqlite3.

## Full feature set

See v1.0.0 release notes for the complete feature list.
