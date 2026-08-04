#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P7 — Full Independent Deployment
#
#  Complete deployment flow without Magisk.
#  Requires: unlocked bootloader device + fastboot tool.
#
#  Usage:
#    ./scripts/p7_full_deploy.sh <vendor_boot.img> [vbmeta.img]
# ═══════════════════════════════════════════════════════
set -e

VENDOR_BOOT="${1:-}"
VBMETA="${2:-}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

pass() { echo -e "${GREEN}OK: $1${NC}"; }
fail() { echo -e "${RED}FAIL: $1${NC}"; exit 1; }
info() { echo -e "${YELLOW}> $1${NC}"; }

echo "=========================================="
echo "  AstraRoot P7 — Full Independent Deploy"
echo "=========================================="
echo ""

# -- check input --
if [ -z "$VENDOR_BOOT" ]; then
    echo "Usage: $0 <vendor_boot.img> [vbmeta.img]"
    echo ""
    echo "To extract vendor_boot from device:"
    echo "  adb shell su -c 'dd if=/dev/block/by-name/vendor_boot of=/sdcard/vendor_boot.img'"
    echo "  adb pull /sdcard/vendor_boot.img"
    echo ""
    echo "Or via fastboot:"
    echo "  adb reboot bootloader"
    echo "  fastboot get_staged vendor_boot.img"
    exit 1
fi

if [ ! -f "$VENDOR_BOOT" ]; then
    fail "vendor_boot.img not found: $VENDOR_BOOT"
fi
pass "vendor_boot: $VENDOR_BOOT"

# -- check kernel module --
KO_PATH="$PROJECT_DIR/kernel/astra_root.ko"
if [ ! -f "$KO_PATH" ]; then
    fail "astra_root.ko not found. Build it first (P1)."
fi
pass "astra_root.ko found"

# -- check astrad --
ASTRAD_PATH=""
for candidate in \
    "$PROJECT_DIR/daemon/build/astrad" \
    "$PROJECT_DIR/daemon/build-arm64/astrad"; do
    if [ -f "$candidate" ]; then
        ASTRAD_PATH="$candidate"
        break
    fi
done
if [ -n "$ASTRAD_PATH" ]; then
    pass "astrad found: $ASTRAD_PATH"
else
    echo -e "${YELLOW}WARN: astrad not found (optional, will load from /dev/astra/)${NC}"
fi

# -- check magiskboot --
if ! command -v magiskboot >/dev/null 2>&1; then
    fail "magiskboot not found. Download from Magisk releases."
fi
pass "magiskboot available"

# -- check fastboot --
if ! command -v fastboot >/dev/null 2>&1; then
    fail "fastboot not found. Install Android platform-tools."
fi
pass "fastboot available"

# -- patch --
info "Patching vendor_boot..."
OUTPUT="vendor_boot_astra_patched.img"

python3 "$SCRIPT_DIR/patch_boot.py" \
    --boot "$VENDOR_BOOT" \
    --module "$KO_PATH" \
    ${ASTRAD_PATH:+--daemon "$ASTRAD_PATH"} \
    --output "$OUTPUT" \
    ${VBMETA:+--vbmeta "$VBMETA"}

if [ ! -f "$OUTPUT" ]; then
    fail "Patch failed, output not generated"
fi
pass "Patched: $OUTPUT"

# -- flash --
echo ""
info "Ready to flash. Device must be in bootloader mode."
echo ""
echo "  Commands to execute:"
echo "    adb reboot bootloader"
echo "    fastboot flash vendor_boot $OUTPUT"
if [ -n "$VBMETA" ]; then
    VBMETA_OUT="${OUTPUT%.img}_vbmeta.img"
    echo "    fastboot flash vbmeta --disable-verity --disable-verification $VBMETA_OUT"
fi
echo "    fastboot reboot"
echo ""
read -p "Flash now? (device must be in bootloader) [y/N] " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    info "Flashing..."
    fastboot flash vendor_boot "$OUTPUT"
    if [ -n "$VBMETA" ] && [ -f "${OUTPUT%.img}_vbmeta.img" ]; then
        fastboot flash vbmeta --disable-verity --disable-verification "${OUTPUT%.img}_vbmeta.img"
    fi
    fastboot reboot
    pass "Flashed and rebooting!"
    echo ""
    echo "  Wait for boot, then verify:"
    echo "    adb shell ls -la /dev/astra_root"
    echo "    adb shell pidof astrad"
    echo "    adb shell cat /dev/astra/boot.log"
else
    echo "  Flash manually when ready."
fi
