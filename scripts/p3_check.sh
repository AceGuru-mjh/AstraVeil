#!/bin/bash
# Quick daemon status check
set -e

echo "-- AstraRoot Daemon Status --"
echo ""

# Is daemon running?
PID=$(adb shell "su -c 'pidof astrad'" 2>/dev/null | tr -d '\r')
if [ -n "$PID" ]; then
    echo "OK: astrad running (pid=$PID)"
else
    echo "FAIL: astrad NOT running"
    echo "   Start: ./scripts/p3_deploy.sh"
    exit 1
fi

# Binary exists?
BIN_CHECK=$(adb shell "su -c 'ls -la /dev/astra/astrad 2>/dev/null'" | tr -d '\r')
if [ -n "$BIN_CHECK" ]; then
    echo "OK: $BIN_CHECK"
else
    echo "FAIL: /dev/astra/astrad missing"
    exit 1
fi

# IPC socket?
SOCKET=$(adb shell "su -c 'ls -la /dev/astra/astrad.sock 2>/dev/null'" | tr -d '\r')
if [ -n "$SOCKET" ]; then
    echo "OK: IPC socket: $SOCKET"
else
    echo "WARN: IPC socket not found"
fi

# Last log lines
echo ""
echo "-- Daemon Log (last 5 lines) --"
adb shell "su -c 'cat /data/local/tmp/astrad.log 2>/dev/null'" | tr -d '\r' | tail -5
