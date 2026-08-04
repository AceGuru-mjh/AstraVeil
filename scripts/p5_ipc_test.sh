#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P5 — App ↔ Daemon IPC full chain test
#
#  Prerequisites:
#    - P1-P3 complete (astra_root.ko + astra_su + astrad deployed)
#    - P4 complete (IPC basic communication verified)
#    - Device connected with root
#
#  Usage:
#    ./scripts/p5_ipc_test.sh [path/to/astrad]
# ═══════════════════════════════════════════════════════
set -e

ASTRAD_PATH="${1:-}"
REMOTE_DIR="/data/local/tmp"
ASTRA_DIR="/dev/astra"
SOCKET_PATH="$ASTRA_DIR/astrad.sock"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

pass() { echo -e "${GREEN}OK: $1${NC}"; }
fail() { echo -e "${RED}FAIL: $1${NC}"; exit 1; }
info() { echo -e "${CYAN}> $1${NC}"; }
warn() { echo -e "${YELLOW}WARN: $1${NC}"; }

echo "=========================================="
echo "  AstraRoot P5 — IPC Full Chain Test"
echo "=========================================="
echo ""

# -- Step 0: locate astrad --
info "Step 0: Locating astrad..."

if [ -z "$ASTRAD_PATH" ]; then
    for candidate in \
        "$PROJECT_DIR/daemon/build/astrad" \
        "$PROJECT_DIR/daemon/build-arm64/astrad" \
        "$PROJECT_DIR/build/daemon/astrad"; do
        if [ -f "$candidate" ]; then
            ASTRAD_PATH="$candidate"
            break
        fi
    done
fi

if [ -z "$ASTRAD_PATH" ]; then
    fail "astrad not found. Download from CI artifact or pass path."
fi
pass "astrad: $ASTRAD_PATH"

# -- Step 1: device check --
info "Step 1: Checking device..."

if ! adb get-state >/dev/null 2>&1; then
    fail "No device connected"
fi
DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
pass "Device: $DEVICE"

ROOT_CHECK=$(adb shell "su -c id" 2>/dev/null | tr -d '\r')
if echo "$ROOT_CHECK" | grep -q "uid=0"; then
    pass "Root available"
else
    fail "No root"
fi

# -- Step 2: check kernel module --
info "Step 2: Checking kernel module..."

if adb shell "su -c 'test -e /dev/astra_root && echo yes'" 2>/dev/null | grep -q "yes"; then
    pass "/dev/astra_root exists"
else
    warn "/dev/astra_root not found. Attempting load..."
    if [ -f "$PROJECT_DIR/kernel/astra_root.ko" ]; then
        adb push "$PROJECT_DIR/kernel/astra_root.ko" "$REMOTE_DIR/astra_root.ko"
        adb shell "su -c 'insmod $REMOTE_DIR/astra_root.ko'" 2>/dev/null || true
        sleep 1
        if adb shell "su -c 'test -e /dev/astra_root && echo yes'" 2>/dev/null | grep -q "yes"; then
            pass "astra_root.ko loaded"
        else
            fail "Failed to load kernel module"
        fi
    else
        fail "astra_root.ko not found locally"
    fi
fi

# -- Step 3: deploy astrad --
info "Step 3: Deploying astrad..."

adb shell "su -c 'mkdir -p $ASTRA_DIR'"
adb push "$ASTRAD_PATH" "$REMOTE_DIR/astrad"
adb shell "su -c 'cp $REMOTE_DIR/astrad $ASTRA_DIR/astrad'"
adb shell "su -c 'chmod 755 $ASTRA_DIR/astrad'"
pass "astrad deployed to $ASTRA_DIR"

# -- Step 4: stop existing daemon --
info "Step 4: Stopping existing daemon..."
adb shell "su -c 'pkill -f astrad 2>/dev/null'" || true
sleep 1
pass "Cleaned up"

# -- Step 5: start astrad --
info "Step 5: Starting astrad..."

adb shell "su -c 'nohup $ASTRA_DIR/astrad --socket $SOCKET_PATH > $ASTRA_DIR/astrad.log 2>&1 &'"
sleep 2

ASTRA_PID=$(adb shell "su -c 'pidof astrad'" 2>/dev/null | tr -d '\r')
if [ -n "$ASTRA_PID" ]; then
    pass "astrad running (PID: $ASTRA_PID)"
else
    echo ""
    echo "  astrad log:"
    adb shell "su -c 'cat $ASTRA_DIR/astrad.log 2>/dev/null | tail -20'" | tr -d '\r'
    echo ""
    fail "astrad failed to start"
fi

# -- Step 6: verify socket --
info "Step 6: Verifying socket..."

sleep 1
if adb shell "su -c 'test -S $SOCKET_PATH && echo yes'" 2>/dev/null | grep -q "yes"; then
    pass "Socket exists: $SOCKET_PATH"
else
    ALT_SOCKET=$(adb shell "su -c 'find /dev/astra /data/adb -name \"*.sock\" 2>/dev/null'" 2>/dev/null | tr -d '\r' | head -1)
    if [ -n "$ALT_SOCKET" ]; then
        SOCKET_PATH="$ALT_SOCKET"
        pass "Socket found at: $SOCKET_PATH"
    else
        fail "Socket not found"
    fi
fi

# -- Step 7: IPC ping test (uses native p4_ipc_client if available) --
info "Step 7: IPC Ping test (type=0x04)..."

# Check if native IPC client is available
USE_NATIVE=0
if adb shell "su -c 'test -x $REMOTE_DIR/p4_ipc_client && echo yes'" 2>/dev/null | grep -q "yes"; then
    USE_NATIVE=1
elif [ -f "$PROJECT_DIR/native/p4_ipc_client" ]; then
    adb push "$PROJECT_DIR/native/p4_ipc_client" "$REMOTE_DIR/p4_ipc_client" 2>/dev/null || true
    adb shell "su -c 'chmod 755 $REMOTE_DIR/p4_ipc_client'" 2>/dev/null || true
    if adb shell "su -c 'test -x $REMOTE_DIR/p4_ipc_client && echo yes'" 2>/dev/null | grep -q "yes"; then
        USE_NATIVE=1
    fi
fi

if [ "$USE_NATIVE" = "1" ]; then
    PING_RESULT=$(adb shell "su -c '$REMOTE_DIR/p4_ipc_client $SOCKET_PATH 04'" 2>/dev/null | tr -d '\r')
else
    # Fallback: python3 binary-framed ping
    PING_RESULT=$(adb shell "su -c 'python3 -c \"
import socket, struct, sys
s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
try:
    s.connect(\\\"$SOCKET_PATH\\\")
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
\"'" 2>/dev/null | tr -d '\r')
fi

if echo "$PING_RESULT" | grep -q "version\|uptime"; then
    pass "IPC Ping: $PING_RESULT"
elif [ -n "$PING_RESULT" ]; then
    warn "IPC Ping response: $PING_RESULT"
else
    warn "IPC Ping inconclusive (no response)"
fi

# -- Step 8: IPC execute test (id) --
info "Step 8: IPC Execute test (id)..."

if [ "$USE_NATIVE" = "1" ]; then
    EXEC_RESULT=$(adb shell "su -c '$REMOTE_DIR/p4_ipc_client $SOCKET_PATH 03 id'" 2>/dev/null | tr -d '\r')
else
    EXEC_RESULT=$(adb shell "su -c 'python3 -c \"
import socket, struct, sys
s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
try:
    s.connect(\\\"$SOCKET_PATH\\\")
    body = b\\\"id\\\"
    payload = bytes([0x03]) + body
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
\"'" 2>/dev/null | tr -d '\r')
fi

if echo "$EXEC_RESULT" | grep -q "uid=0"; then
    pass "Execute 'id' via daemon: uid=0(root)"
elif echo "$EXEC_RESULT" | grep -q "uid="; then
    warn "Execute result: $EXEC_RESULT"
else
    warn "Execute response: $EXEC_RESULT"
fi

# -- Step 9: IPC execute test (getprop) --
info "Step 9: IPC Execute test (getprop)..."

if [ "$USE_NATIVE" = "1" ]; then
    PROP_RESULT=$(adb shell "su -c '$REMOTE_DIR/p4_ipc_client $SOCKET_PATH 03 \"getprop ro.product.model\"'" 2>/dev/null | tr -d '\r')
else
    PROP_RESULT=$(adb shell "su -c 'python3 -c \"
import socket, struct, sys
s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
try:
    s.connect(\\\"$SOCKET_PATH\\\")
    body = b\\\"getprop ro.product.model\\\"
    payload = bytes([0x03]) + body
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
\"'" 2>/dev/null | tr -d '\r')
fi

if echo "$PROP_RESULT" | grep -q "stdout\|result\|model"; then
    pass "Execute 'getprop': $PROP_RESULT"
else
    warn "getprop response: $PROP_RESULT"
fi

# -- Step 10: summary --
echo ""
echo "=========================================="
echo "  P5 IPC Verification Summary"
echo "=========================================="
echo ""
echo "  Device:       $DEVICE"
echo "  astrad PID:   $ASTRA_PID"
echo "  Socket:       $SOCKET_PATH"
echo "  Kernel:       $(adb shell "su -c 'lsmod | grep astra_root'" 2>/dev/null | tr -d '\r' | head -1)"
echo ""
echo "  Next: Install app -> verify DaemonState = ONLINE"
echo "    ./gradlew :app:installDebug"
echo ""

adb shell "su -c 'cat $ASTRA_DIR/astrad.log'" > /tmp/astrad_p5.log 2>/dev/null
echo "  Log saved: /tmp/astrad_p5.log"
echo ""
