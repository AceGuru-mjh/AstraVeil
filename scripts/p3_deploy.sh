#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P3 — Daemon (astrad) Device Deployment
#
#  Deploys the cross-compiled astrad binary to the device and starts
#  it as a background service. Requires:
#    - P2 complete (astra_root.ko loaded, /dev/astra_root active)
#    - astrad ARM64 binary (from CI artifact or local cross-compile)
#    - Device connected via adb with root access
#
#  Usage:
#    ./p3_deploy.sh [path/to/astrad]
#
#  If no path given, looks for astrad in:
#    1. daemon/build-arm64/astrad (local cross-compile output)
#    2. CI artifact download location
# ═══════════════════════════════════════════════════════
set -e

REMOTE_DIR="/data/local/tmp"
ASTRA_SOCKET="/dev/astra/astrad.sock"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}OK: $1${NC}"; }
fail() { echo -e "${RED}FAIL: $1${NC}"; exit 1; }
info() { echo -e "${YELLOW}> $1${NC}"; }

echo "=========================================="
echo "  AstraRoot P3 — Daemon Deployment"
echo "=========================================="
echo ""

# ── Step 0: Find astrad binary ──
info "Step 0: Locate astrad binary..."

ASTRAD=""
# Check argument
if [ -n "$1" ] && [ -f "$1" ]; then
    ASTRAD="$1"
# Check local cross-compile output
elif [ -f "$PROJECT_DIR/daemon/build-arm64/astrad" ]; then
    ASTRAD="$PROJECT_DIR/daemon/build-arm64/astrad"
# Check common download locations
elif [ -f "$PROJECT_DIR/astrad" ]; then
    ASTRAD="$PROJECT_DIR/astrad"
elif [ -f "$PROJECT_DIR/astrad-arm64" ]; then
    ASTRAD="$PROJECT_DIR/astrad-arm64"
fi

if [ -z "$ASTRAD" ]; then
    fail "astrad binary not found.
   Options:
   1. Pass path as argument: ./p3_deploy.sh /path/to/astrad
   2. Download from CI: https://github.com/AceGuru-mjh/AstraVeil/actions/workflows/daemon.yml
   3. Cross-compile locally:
      cmake -S daemon -B daemon/build-arm64 \\
        -DCMAKE_TOOLCHAIN_FILE=\$NDK/build/cmake/android.toolchain.cmake \\
        -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29 \\
        -DCMAKE_BUILD_TYPE=Release -DASTRA_BUILD_TESTS=OFF
      cmake --build daemon/build-arm64 -j"
fi

# Verify it's ARM64
if ! file "$ASTRAD" | grep -q "ARM aarch64"; then
    fail "astrad is not ARM64: $(file "$ASTRAD")"
fi

pass "astrad found: $ASTRAD ($(stat -c%s "$ASTRAD" 2>/dev/null || stat -f%z "$ASTRAD") bytes)"

# ── Step 1: Check prerequisites ──
info "Step 1: Check prerequisites..."

if ! adb get-state >/dev/null 2>&1; then
    fail "No device connected. Run: adb devices"
fi
DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
pass "Device connected: $DEVICE"

# Check root
ROOT_CHECK=$(adb shell "su -c id" 2>/dev/null | tr -d '\r')
if echo "$ROOT_CHECK" | grep -q "uid=0"; then
    pass "Device has root"
else
    fail "Device does not have root. P3 requires root for daemon deployment."
fi

# Check if P2 is complete (astra_root.ko loaded)
LSMOD=$(adb shell "su -c 'lsmod'" 2>/dev/null | tr -d '\r')
if echo "$LSMOD" | grep -q "astra_root"; then
    pass "astra_root.ko is loaded (P2 complete)"
else
    echo -e "${YELLOW}WARN: astra_root.ko not loaded. P2 not complete.${NC}"
    echo "  The daemon can still run but won't be able to use AstraRoot provider."
    echo "  Run P2 first: ./scripts/p2_deploy.sh"
    echo ""
fi

# ── Step 2: Push astrad to device ──
info "Step 2: Push astrad to device..."

adb push "$ASTRAD" "$REMOTE_DIR/astrad"
adb shell "su -c 'chmod 755 $REMOTE_DIR/astrad'"
pass "astrad pushed to $REMOTE_DIR/astrad"

# ── Step 3: Create daemon directory ──
info "Step 3: Create /dev/astra/ directory..."

adb shell "su -c 'mkdir -p /dev/astra'"
adb shell "su -c 'chmod 755 /dev/astra'"
pass "/dev/astra/ created"

# ── Step 4: Install astrad to /dev/astra/ ──
info "Step 4: Install astrad to /dev/astra/..."

# Copy to persistent location
adb shell "su -c 'cp $REMOTE_DIR/astrad /dev/astra/astrad'"
adb shell "su -c 'chmod 755 /dev/astra/astrad'"
pass "astrad installed to /dev/astra/astrad"

# ── Step 5: Stop existing daemon (if running) ──
info "Step 5: Stop existing daemon..."

adb shell "su -c 'pkill -f astrad 2>/dev/null'" || true
sleep 1

# Check if still running
PID=$(adb shell "su -c 'pidof astrad'" 2>/dev/null | tr -d '\r')
if [ -n "$PID" ]; then
    adb shell "su -c 'kill -9 $PID 2>/dev/null'" || true
    sleep 1
fi
pass "Existing daemon stopped"

# ── Step 6: Start daemon ──
info "Step 6: Start astrad daemon..."

# Start in background with logging
adb shell "su -c 'nohup /dev/astra/astrad --socket $ASTRA_SOCKET > /data/local/tmp/astrad.log 2>&1 &'" &
sleep 2

# Check if running
PID=$(adb shell "su -c 'pidof astrad'" 2>/dev/null | tr -d '\r')
if [ -n "$PID" ]; then
    pass "astrad running (pid=$PID)"
else
    echo -e "${RED}FAIL: astrad failed to start${NC}"
    echo ""
    echo "=== astrad log ==="
    adb shell "su -c 'cat /data/local/tmp/astrad.log 2>/dev/null'" | tr -d '\r' | tail -20
    fail "Check the log above for errors"
fi

# ── Step 7: Verify IPC socket ──
info "Step 7: Verify IPC socket..."

sleep 1
SOCKET_CHECK=$(adb shell "su -c 'ls -la $ASTRA_SOCKET 2>/dev/null'" | tr -d '\r')
if [ -n "$SOCKET_CHECK" ]; then
    pass "IPC socket: $SOCKET_CHECK"
else
    echo -e "${YELLOW}WARN: IPC socket not found at $ASTRA_SOCKET${NC}"
    echo "  Daemon may still be initializing. Check log:"
    adb shell "su -c 'cat /data/local/tmp/astrad.log 2>/dev/null'" | tr -d '\r' | tail -10
fi

# ── Step 8: Test daemon IPC ──
info "Step 8: Test daemon IPC..."

# Try a simple version query via the socket
IPC_TEST=$(adb shell "su -c 'echo '{\"type\":\"version\"}' | nc -U $ASTRA_SOCKET 2>/dev/null'" 2>/dev/null | tr -d '\r')
if [ -n "$IPC_TEST" ]; then
    pass "IPC response: $IPC_TEST"
else
    echo -e "${YELLOW}WARN: No IPC response (nc may not support -U)${NC}"
    echo "  This is OK — the daemon is running, IPC can be tested from the app."
fi

# ── Step 9: Show status ──
info "Step 9: Daemon status..."

echo ""
echo "=== Process ==="
adb shell "su -c 'ps -A | grep astrad'" 2>/dev/null | tr -d '\r'
echo ""
echo "=== Log (last 10 lines) ==="
adb shell "su -c 'cat /data/local/tmp/astrad.log 2>/dev/null'" | tr -d '\r' | tail -10
echo ""

# ── Done ──
echo ""
echo "=========================================="
echo "  P3 DEPLOYMENT COMPLETE"
echo "=========================================="
echo ""
echo "  Daemon:    /dev/astra/astrad"
echo "  Socket:    $ASTRA_SOCKET"
echo "  PID:       $PID"
echo "  Log:       /data/local/tmp/astrad.log"
echo ""
echo "  Next: P4 — App ↔ Daemon IPC full chain"
echo ""
echo "  To stop daemon:"
echo "    adb shell su -c 'pkill astrad'"
echo ""
echo "  To view log:"
echo "    adb shell su -c 'cat /data/local/tmp/astrad.log'"
echo ""
