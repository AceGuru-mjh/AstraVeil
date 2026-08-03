#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P4 — App integration test
#
#  Builds and installs the AstraVeil app, launches it, and checks
#  that it connects to the daemon via logcat.
#
#  Prerequisites:
#    - P4 daemon running (./scripts/p4_verify.sh)
#    - Android SDK + NDK configured for Gradle build
# ═══════════════════════════════════════════════════════
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "  AstraRoot P4 — App Integration Test"
echo "=========================================="
echo ""

# ── Step 1: Build + install App ──
echo "-- Step 1: Build + install App --"
cd "$PROJECT_DIR"
./gradlew :app:installDebug 2>&1 | tail -5
echo ""

# ── Step 2: Launch App ──
echo "-- Step 2: Launch App --"
adb shell am start -n com.astraveil.app/.MainActivity 2>&1
sleep 3
echo "App launched"
echo ""

# ── Step 3: Check daemon connection via logcat ──
echo "-- Step 3: Check daemon connection (logcat) --"
adb logcat -d -t 100 2>/dev/null | grep -iE "AstraDaemonClient|DaemonManager|DaemonState|astrad|daemon.*connect" | tail -15
echo ""

# ── Step 4: Check if daemon state is ONLINE ──
echo "-- Step 4: Daemon state check --"
ONLINE_CHECK=$(adb logcat -d -t 200 2>/dev/null | grep -i "DaemonState.*ONLINE\|daemon.*connected\|Daemon connected" | tail -5)
if [ -n "$ONLINE_CHECK" ]; then
    echo "OK: Daemon appears connected"
    echo "  $ONLINE_CHECK"
else
    echo "WARN: No ONLINE state found in logcat"
    echo "  Check the app manually — Dashboard should show 'AstraDaemon: Online'"
fi
echo ""

# ── Step 5: Manual verification checklist ──
echo "-- Step 5: Manual verification --"
echo ""
echo "  Open the AstraVeil app and verify:"
echo ""
echo "  [ ] Dashboard shows 'AstraDaemon: Online'"
echo "  [ ] System Status card shows daemon info"
echo "  [ ] Terminal tab works (type 'id')"
echo "  [ ] Terminal returns 'uid=0(root)' (if kernel module loaded)"
echo ""
echo "  To view daemon logs:"
echo "    adb shell su -c 'cat /data/local/tmp/astrad.log'"
echo ""
echo "  To view app logs:"
echo "    adb logcat -s AstraVeilApp:AstraDaemonClient:DaemonManager:*:S"
echo ""
echo "=========================================="
echo "  Done"
echo "=========================================="
