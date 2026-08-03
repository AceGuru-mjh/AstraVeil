#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P4 — IPC protocol test
#
#  Tests individual IPC message types against the running daemon.
#  Uses the native p4_ipc_client if available, falls back to python3.
#
#  Usage: ./p4_ipc_test.sh [socket_path]
# ═══════════════════════════════════════════════════════
set -e

SOCKET="${1:-/dev/astra/astrad.sock}"
REMOTE_DIR="/data/local/tmp"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "=========================================="
echo "  AstraRoot P4 — IPC Protocol Test"
echo "  Socket: $SOCKET"
echo "=========================================="
echo ""

# Determine test method
USE_NATIVE=0
if adb shell "su -c 'test -x $REMOTE_DIR/p4_ipc_client && echo yes'" 2>/dev/null | grep -q "yes"; then
    USE_NATIVE=1
    echo "Using native p4_ipc_client"
elif [ -f "$PROJECT_DIR/native/p4_ipc_client" ]; then
    adb push "$PROJECT_DIR/native/p4_ipc_client" "$REMOTE_DIR/p4_ipc_client" 2>/dev/null || true
    adb shell "su -c 'chmod 755 $REMOTE_DIR/p4_ipc_client'" 2>/dev/null || true
    if adb shell "su -c 'test -x $REMOTE_DIR/p4_ipc_client && echo yes'" 2>/dev/null | grep -q "yes"; then
        USE_NATIVE=1
        echo "Using native p4_ipc_client (just pushed)"
    fi
fi

if [ "$USE_NATIVE" = "0" ]; then
    echo "Using python3 fallback (native client not available)"
fi
echo ""

run_native() {
    local type="$1"
    local body="${2:-}"
    if [ -n "$body" ]; then
        adb shell "su -c '$REMOTE_DIR/p4_ipc_client $SOCKET $type \"$body\"'" 2>/dev/null | tr -d '\r'
    else
        adb shell "su -c '$REMOTE_DIR/p4_ipc_client $SOCKET $type'" 2>/dev/null | tr -d '\r'
    fi
}

run_python() {
    local type="$1"
    local body="${2:-}"
    adb shell "su -c 'python3 -c \"
import socket, struct, sys
s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
try:
    s.connect(\\\"$SOCKET\\\")
    body_bytes = b\\\"$body\\\"
    payload = bytes([0x$type]) + body_bytes
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
\"'" 2>/dev/null | tr -d '\r'
}

test_ipc() {
    local name="$1"
    local type="$2"
    local body="${3:-}"
    echo "-- Test: $name (type=0x$type) --"
    if [ "$USE_NATIVE" = "1" ]; then
        result=$(run_native "$type" "$body")
    else
        result=$(run_python "$type" "$body")
    fi
    if [ -n "$result" ]; then
        echo "  Response: $result"
    else
        echo "  (no response)"
    fi
    echo ""
}

# Test 1: Ping
test_ipc "Ping" "04"

# Test 2: GetCapabilityMatrix
test_ipc "Get Capability Matrix" "05"

# Test 3: Execute 'id'
test_ipc "Execute 'id'" "03" "id"

# Test 4: Execute 'getprop ro.product.model'
test_ipc "Execute 'getprop ro.product.model'" "03" "getprop ro.product.model"

# Test 5: GetCapability (body = capability name)
test_ipc "Get Capability 'SHELL'" "01" "SHELL"

# Test 6: GetProvider (body = capability name)
test_ipc "Get Provider 'SHELL'" "02" "SHELL"

echo "=========================================="
echo "  Done"
echo "=========================================="
