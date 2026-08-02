#!/bin/bash
# Quick status check (does not redeploy, only checks current state)
set -e

echo "-- AstraRoot Status --"
echo ""

# Is module loaded?
if adb shell "su -c 'lsmod'" 2>/dev/null | grep -q "astra_root"; then
    echo "OK: Module loaded"
else
    echo "FAIL: Module NOT loaded"
    echo "   Fix: adb shell su -c 'insmod /data/local/tmp/astra_root.ko'"
    exit 1
fi

# Device node?
if adb shell "su -c 'test -e /dev/astra_root && echo yes'" 2>/dev/null | grep -q "yes"; then
    PERMS=$(adb shell "su -c 'ls -la /dev/astra_root'" 2>/dev/null | tr -d '\r')
    echo "OK: /dev/astra_root: $PERMS"
else
    echo "FAIL: /dev/astra_root missing"
    exit 1
fi

# dmesg last few lines
echo ""
echo "-- Kernel Log --"
adb shell "su -c 'dmesg | grep astra_root | tail -5'" 2>/dev/null | tr -d '\r'

# Quick privilege test
echo ""
echo "-- Quick Root Test --"
RESULT=$(adb shell "su -c '/data/local/tmp/astra_su -c id'" 2>/dev/null | tr -d '\r')
echo "  $RESULT"

if echo "$RESULT" | grep -q "uid=0"; then
    echo "  OK: Root working"
else
    echo "  FAIL: Root NOT working"
fi
