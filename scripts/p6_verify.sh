#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P6 — Post-reboot verification
#  Usage: run this after rebooting the device
# ═══════════════════════════════════════════════════════

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}OK: $1${NC}"; }
fail() { echo -e "${RED}FAIL: $1${NC}"; }
info() { echo -e "${YELLOW}> $1${NC}"; }

echo "=========================================="
echo "  P6 Boot Integration Verify"
echo "=========================================="
echo ""

info "astrad process..."
PID=$(adb shell "su -c 'pidof astrad'" 2>/dev/null | tr -d '\r')
if [ -n "$PID" ]; then
    pass "astrad running (PID: $PID)"
else
    fail "astrad NOT running"
fi
echo ""

info "Socket..."
if adb shell "su -c 'test -S /dev/astra/astrad.sock && echo yes'" 2>/dev/null | grep -q "yes"; then
    pass "Socket exists"
else
    fail "Socket NOT found"
fi
echo ""

info "Kernel module..."
if adb shell "su -c 'test -e /dev/astra_root && echo yes'" 2>/dev/null | grep -q "yes"; then
    pass "/dev/astra_root exists"
else
    fail "/dev/astra_root NOT found"
fi
echo ""

info "Boot log..."
adb shell "su -c 'cat /dev/astra/boot.log 2>/dev/null | tail -10'" | tr -d '\r'
echo ""

info "Quick IPC test..."
RESULT=$(adb shell "su -c '
if command -v python3 >/dev/null 2>&1; then
    python3 -c \"
import socket, struct, sys
s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
try:
    s.connect(\\\"/dev/astra/astrad.sock\\\")
    payload = bytes([0x04])
    s.send(struct.pack(\\\">I\\\", len(payload)) + payload)
    net_len = s.recv(4)
    resp_len = struct.unpack(\\\">I\\\", net_len)[0]
    resp = b\\\"
    while len(resp) < resp_len:
        chunk = s.recv(resp_len - len(resp))
        if not chunk: break
        resp += chunk
    sys.stdout.write(resp[1:].decode())
finally:
    s.close()
\"
else
    echo \"{\\\"error\\\":\\\"no python3\\\"}\"
fi
'" 2>/dev/null | tr -d '\r')
if [ -n "$RESULT" ]; then
    pass "IPC response: $RESULT"
else
    fail "No IPC response"
fi
echo ""

echo "=========================================="
echo "  Done"
echo "=========================================="
