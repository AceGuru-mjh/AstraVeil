#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P5 — App-side integration verification
#  Prerequisite: astrad running (P5 IPC test passed)
# ═══════════════════════════════════════════════════════
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}OK: $1${NC}"; }
fail() { echo -e "${RED}FAIL: $1${NC}"; exit 1; }
info() { echo -e "${YELLOW}> $1${NC}"; }

echo "=========================================="
echo "  P5 App Integration Verify"
echo "=========================================="
echo ""

# -- install app --
info "Install App..."
./gradlew :app:installDebug 2>&1 | tail -3
echo ""

# -- launch --
info "Launch..."
adb shell am start -n com.astraveil.app/.MainActivity
sleep 3
echo ""

# -- check logcat --
info "Daemon connection log..."
adb logcat -d -t 30 2>/dev/null | grep -i "astra\|daemon\|socket\|ipc" | tail -10
echo ""

echo "=========================================="
echo "  Manual checks"
echo "=========================================="
echo "  [ ] Dashboard shows 'AstraDaemon: Online'"
echo "  [ ] Terminal -> type 'id' -> uid=0(root)"
echo "  [ ] Superuser -> policies list loads"
echo ""
