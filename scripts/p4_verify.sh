#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P4 — App ↔ Daemon IPC full chain verification
#
#  Verifies the complete chain:
#    App (AstraDaemonClient) → astrad (daemon) → astra_root.ko → kernel
#
#  Prerequisites:
#    - P1: astra_root.ko compiled (CI artifact or local)
#    - P2: astra_su + test_root compiled
#    - P3: astrad ARM64 compiled (CI artifact)
#    - Device connected via adb with root
#
#  Usage:
#    ./p4_verify.sh [path/to/astrad]
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
echo "  AstraRoot P4 — Full Chain Verification"
echo "=========================================="
echo ""

# ── Step 0: Locate astrad ──
info "Step 0: Locating astrad binary..."

if [ -z "$ASTRAD_PATH" ]; then
    for candidate in \
        "$PROJECT_DIR/daemon/build-arm64/astrad" \
        "$PROJECT_DIR/daemon/build/astrad" \
        "$PROJECT_DIR/astrad" \
        "$PROJECT_DIR/astrad-arm64"; do
        if [ -f "$candidate" ]; then
            ASTRAD_PATH="$candidate"
            break
        fi
    done
fi

if [ -z "$ASTRAD_PATH" ]; then
    fail "astrad binary not found. Download from CI: daemon.yml → astrad-arm64 artifact"
fi
pass "astrad found: $ASTRAD_PATH"

# ── Step 1: Device check ──
info "Step 1: Checking device..."

if ! adb get-state >/dev/null 2>&1; then
    fail "No device connected"
fi
DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
pass "Device: $DEVICE"

ROOT_CHECK=$(adb shell "su -c id" 2>/dev/null | tr -d '\r')
echo "$ROOT_CHECK" | grep -q "uid=0" && pass "Device has root" || fail "Device does not have root"

# ── Step 2: Kernel module ──
info "Step 2: Checking kernel module..."

KO_CHECK=$(adb shell "su -c 'lsmod'" 2>/dev/null | tr -d '\r')
if echo "$KO_CHECK" | grep -q "astra_root"; then
    pass "astra_root.ko loaded"
else
    warn "astra_root.ko not loaded — attempting to load..."
    for ko_candidate in \
        "$PROJECT_DIR/kernel/astra_root.ko" \
        "$REMOTE_DIR/astra_root.ko"; do
        if [ -f "$ko_candidate" ]; then
            adb push "$ko_candidate" "$REMOTE_DIR/astra_root.ko"
            adb shell "su -c 'insmod $REMOTE_DIR/astra_root.ko'" 2>&1 || true
            sleep 1
            break
        fi
    done
    if adb shell "su -c 'lsmod'" 2>/dev/null | grep -q "astra_root"; then
        pass "astra_root.ko loaded"
    else
        warn "astra_root.ko not loaded (daemon will run but no root escalation)"
    fi
fi

# Check /dev/astra_root
if adb shell "su -c 'test -e /dev/astra_root && echo yes'" 2>/dev/null | grep -q "yes"; then
    pass "/dev/astra_root exists"
else
    warn "/dev/astra_root not found"
fi

# ── Step 3: Deploy astrad ──
info "Step 3: Deploying astrad..."

adb push "$ASTRAD_PATH" "$REMOTE_DIR/astrad" 2>&1 | tail -1
adb shell "su -c 'chmod 755 $REMOTE_DIR/astrad'"
pass "astrad pushed"

# ── Step 4: Create socket directory ──
info "Step 4: Creating socket directory..."
adb shell "su -c 'mkdir -p $ASTRA_DIR && chmod 0755 $ASTRA_DIR'"
pass "$ASTRA_DIR ready"

# ── Step 5: Stop existing daemon ──
info "Step 5: Stopping existing daemon..."
adb shell "su -c 'pkill -f astrad 2>/dev/null'" || true
sleep 1
pass "Stopped"

# ── Step 6: Start daemon ──
info "Step 6: Starting astrad..."
adb shell "su -c 'nohup $REMOTE_DIR/astrad --socket $SOCKET_PATH > /data/local/tmp/astrad.log 2>&1 &'" &
sleep 2

ASTRA_PID=$(adb shell "su -c 'pidof astrad'" 2>/dev/null | tr -d '\r')
if [ -n "$ASTRA_PID" ]; then
    pass "astrad running (PID: $ASTRA_PID)"
else
    echo ""
    echo "  astrad log:"
    adb shell "su -c 'cat /data/local/tmp/astrad.log 2>/dev/null | tail -20'" | tr -d '\r'
    fail "astrad failed to start"
fi

# ── Step 7: Verify socket ──
info "Step 7: Verifying IPC socket..."
if adb shell "su -c 'test -S $SOCKET_PATH && echo yes'" 2>/dev/null | grep -q "yes"; then
    SOCK_PERMS=$(adb shell "su -c 'ls -la $SOCKET_PATH'" 2>/dev/null | tr -d '\r')
    pass "Socket: $SOCK_PERMS"
else
    warn "Socket not found at $SOCKET_PATH"
    adb shell "su -c 'find /dev/astra /data/local/tmp -name \"*.sock\" 2>/dev/null'" | tr -d '\r'
    fail "IPC socket not found"
fi

# ── Step 8: Build + push IPC test client ──
info "Step 8: Building IPC test client..."

IPC_CLIENT_SRC="$PROJECT_DIR/native/p4_ipc_client.c"
IPC_CLIENT_LOCAL="$PROJECT_DIR/native/p4_ipc_client"

# Try to cross-compile locally
if [ ! -f "$IPC_CLIENT_LOCAL" ]; then
    if command -v aarch64-linux-gnu-gcc >/dev/null 2>&1; then
        aarch64-linux-gnu-gcc -static -O2 -o "$IPC_CLIENT_LOCAL" "$IPC_CLIENT_SRC" 2>&1 || true
    fi
fi

if [ -f "$IPC_CLIENT_LOCAL" ]; then
    adb push "$IPC_CLIENT_LOCAL" "$REMOTE_DIR/p4_ipc_client" 2>&1 | tail -1
    adb shell "su -c 'chmod 755 $REMOTE_DIR/p4_ipc_client'"
    pass "IPC test client deployed"
    USE_NATIVE_CLIENT=1
else
    warn "IPC test client not built (no cross-compiler). Using python fallback."
    USE_NATIVE_CLIENT=0
fi

# ── Step 9: IPC ping test ──
info "Step 9: IPC ping test (type=0x04)..."

if [ "$USE_NATIVE_CLIENT" = "1" ]; then
    PING_RESULT=$(adb shell "su -c '$REMOTE_DIR/p4_ipc_client $SOCKET_PATH 04'" 2>/dev/null | tr -d '\r')
else
    PING_RESULT=$(adb shell "su -c 'python3 -c \"
import socket, struct, sys
s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
s.connect(\\\"$SOCKET_PATH\\\")
# frame: 4-byte len (big-endian) + 1-byte type + body
payload = bytes([0x04])
s.send(struct.pack(\\\">I\\\", len(payload)) + payload)
# read response
net_len = s.recv(4)
resp_len = struct.unpack(\\\">I\\\", net_len)[0]
resp = s.recv(resp_len)
# skip type byte, print JSON
sys.stdout.write(resp[1:].decode())
s.close()
\"'" 2>/dev/null | tr -d '\r')
fi

if echo "$PING_RESULT" | grep -q "version\|uptime"; then
    pass "IPC ping response: $PING_RESULT"
elif [ -n "$PING_RESULT" ]; then
    warn "IPC ping response: $PING_RESULT"
else
    warn "No IPC response (daemon may not handle ping yet)"
fi

# ── Step 10: Status summary ──
echo ""
echo "=========================================="
echo "  P4 Verification Summary"
echo "=========================================="
echo ""
echo "  Device:        $DEVICE"
echo "  astrad PID:    $ASTRA_PID"
echo "  Socket:        $SOCKET_PATH"
echo "  Kernel module: $(adb shell "su -c 'lsmod | grep astra_root'" 2>/dev/null | tr -d '\r' | head -1 || echo 'not loaded')"
echo ""
echo "  Manual checks:"
echo "    1. Install app: ./gradlew :app:installDebug"
echo "    2. Open app → Dashboard should show 'AstraDaemon: Online'"
echo "    3. Open Terminal → type 'id' → should show uid=0"
echo ""
echo "  Logs:"
echo "    adb shell su -c 'cat /data/local/tmp/astrad.log'"
echo ""
echo "  To stop: adb shell su -c 'pkill astrad'"
echo ""
