#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P2 — Device deployment + verification
#
#  Prerequisites:
#    - P1 compiled astra_root.ko
#    - Device connected (adb devices visible)
#    - Device has Magisk root (for insmod permission)
#
#  Usage:
#    ./p2_deploy.sh [path/to/astra_root.ko]
# ═══════════════════════════════════════════════════════
set -e

KO_PATH="${1:-kernel/astra_root.ko}"
REMOTE_DIR="/data/local/tmp"
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
echo "  AstraRoot P2 — Device Deployment"
echo "=========================================="
echo ""

# -- Step 0: check prerequisites --
info "Step 0: Checking prerequisites..."

if [ ! -f "$KO_PATH" ]; then
    fail "astra_root.ko not found at: $KO_PATH
   Run P1 build first: cd kernel && ./build.sh"
fi
pass "astra_root.ko found: $KO_PATH ($(stat -c%s "$KO_PATH" 2>/dev/null || stat -f%z "$KO_PATH") bytes)"

if ! adb get-state >/dev/null 2>&1; then
    fail "No device connected. Run: adb devices"
fi
DEVICE=$(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r')
pass "Device connected: $DEVICE"

# Check device has root (Magisk/KSU/APatch)
ROOT_CHECK=$(adb shell "su -c id" 2>/dev/null | tr -d '\r')
if echo "$ROOT_CHECK" | grep -q "uid=0"; then
    pass "Device has root (for insmod)"
else
    fail "Device does not have root. P2 requires existing root for insmod.
   Install Magisk first, or wait for P7 (standalone boot patch)."
fi

# -- Step 1: build test tools --
info "Step 1: Building test tools..."

NDK="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/27.0.12077973}"
CC=""

# Find NDK clang
for candidate in \
    "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android31-clang" \
    "$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android31-clang" \
    "$NDK/toolchains/llvm/prebuilt/windows-x86_64/bin/aarch64-linux-android31-clang.exe"; do
    if [ -f "$candidate" ]; then
        CC="$candidate"
        break
    fi
done

if [ -z "$CC" ]; then
    # Try cross-compiler in PATH
    if command -v aarch64-linux-android31-clang >/dev/null 2>&1; then
        CC="aarch64-linux-android31-clang"
    elif command -v aarch64-linux-gnu-gcc >/dev/null 2>&1; then
        CC="aarch64-linux-gnu-gcc"
    else
        fail "No ARM64 cross-compiler found.
   Set ANDROID_NDK_HOME or install NDK r27."
    fi
fi

echo "  Compiler: $CC"

# Build test_root
$CC -static -O2 -o "$PROJECT_DIR/native/test_root" \
    "$PROJECT_DIR/native/test_root.c"
pass "test_root built"

# Build astra_su
$CC -static -O2 -o "$PROJECT_DIR/native/astra_su" \
    "$PROJECT_DIR/native/astra_su.c"
pass "astra_su built"

# -- Step 2: push to device --
info "Step 2: Pushing files to device..."

adb push "$KO_PATH" "$REMOTE_DIR/astra_root.ko"
adb push "$PROJECT_DIR/native/test_root" "$REMOTE_DIR/test_root"
adb push "$PROJECT_DIR/native/astra_su" "$REMOTE_DIR/astra_su"

adb shell "chmod 755 $REMOTE_DIR/test_root $REMOTE_DIR/astra_su"
pass "Files pushed to $REMOTE_DIR"

# -- Step 3: unload old module (if exists) --
info "Step 3: Cleaning up old module..."
adb shell "su -c 'rmmod astra_root 2>/dev/null'" || true
sleep 1

# -- Step 4: load kernel module --
info "Step 4: Loading astra_root.ko..."

LOAD_RESULT=$(adb shell "su -c 'insmod $REMOTE_DIR/astra_root.ko'" 2>&1 | tr -d '\r')
if [ -n "$LOAD_RESULT" ]; then
    echo "  insmod output: $LOAD_RESULT"
fi

sleep 1

# Check module loaded
LSMOD=$(adb shell "su -c 'lsmod'" 2>/dev/null | tr -d '\r')
if echo "$LSMOD" | grep -q "astra_root"; then
    pass "Module loaded (lsmod)"
else
    # lsmod may not be available, check dmesg
    DMESG=$(adb shell "su -c 'dmesg | tail -20'" 2>/dev/null | tr -d '\r')
    if echo "$DMESG" | grep -q "astra_root.*loaded"; then
        pass "Module loaded (dmesg)"
    else
        echo ""
        echo "  dmesg output:"
        echo "$DMESG" | tail -10
        echo ""
        fail "Module failed to load. Check dmesg above."
    fi
fi

# -- Step 5: check device node --
info "Step 5: Checking /dev/astra_root..."

DEV_CHECK=$(adb shell "su -c 'ls -la /dev/astra_root'" 2>/dev/null | tr -d '\r')
if echo "$DEV_CHECK" | grep -q "astra_root"; then
    pass "/dev/astra_root exists: $DEV_CHECK"
else
    fail "/dev/astra_root not found. SELinux may be blocking misc_register.
   Try: adb shell su -c 'setenforce 0' then re-run."
fi

# -- Step 6: run privilege test --
info "Step 6: Running privilege test..."
echo ""

adb shell "su -c '$REMOTE_DIR/test_root -v'"
TEST_EXIT=$?

echo ""
if [ $TEST_EXIT -eq 0 ]; then
    pass "Privilege test PASSED"
else
    fail "Privilege test FAILED (exit code $TEST_EXIT)"
fi

# -- Step 7: test astra_su --
info "Step 7: Testing astra_su..."

SU_ID=$(adb shell "su -c '$REMOTE_DIR/astra_su -c id'" 2>/dev/null | tr -d '\r')
if echo "$SU_ID" | grep -q "uid=0"; then
    pass "astra_su -c id -> $SU_ID"
else
    fail "astra_su failed: $SU_ID"
fi

SU_PROP=$(adb shell "su -c '$REMOTE_DIR/astra_su -c \"getprop ro.product.model\"'" 2>/dev/null | tr -d '\r')
pass "astra_su getprop -> $SU_PROP"

# -- Step 8: test without su prefix (verify direct kernel escalation) --
info "Step 8: Testing without su prefix (direct kernel escalation)..."

# Run astra_su as shell user (UID 2000)
# If kernel module works, astra_su escalates itself, no external su needed
DIRECT_RESULT=$(adb shell "$REMOTE_DIR/astra_su -c id" 2>/dev/null | tr -d '\r')
if echo "$DIRECT_RESULT" | grep -q "uid=0"; then
    pass "Direct escalation works (no su needed): $DIRECT_RESULT"
    echo ""
    echo "  This means astra_root.ko provides root INDEPENDENTLY."
    echo "     Magisk is NOT required for this to work."
else
    echo -e "${YELLOW}WARN: Direct escalation failed: $DIRECT_RESULT${NC}"
    echo "  This is expected if /dev/astra_root has mode 0600."
    echo "  For P2 dev testing, we use 'su -c' wrapper."
    echo "  P3 will fix permissions via init script."
fi

# -- done --
echo ""
echo "=========================================="
echo "  P2 COMPLETE"
echo "=========================================="
echo ""
echo "  Module:    astra_root.ko loaded"
echo "  Device:    /dev/astra_root active"
echo "  Test:      test_root PASSED"
echo "  Su:        astra_su working"
echo ""
echo "  Next: P3 — astrad daemon deployment"
echo ""
echo "  To unload module:"
echo "    adb shell su -c 'rmmod astra_root'"
echo ""
echo "  To check kernel log:"
echo "    adb shell su -c 'dmesg | grep astra_root'"
echo ""
