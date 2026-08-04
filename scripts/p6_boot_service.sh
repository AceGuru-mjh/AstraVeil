#!/bin/bash
# ═══════════════════════════════════════════════════════
#  AstraRoot P6 — Boot Integration
#
#  Auto-start astrad on boot.
#  Dev phase: borrow Magisk's service.sh mechanism.
#  Prod phase: P7 boot patcher injects init script directly.
#
#  Usage:
#    ./scripts/p6_boot_service.sh
# ═══════════════════════════════════════════════════════
set -e

ASTRA_DIR="/dev/astra"
MAGISK_MODULE_DIR="/data/adb/modules/astra_root_service"
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
echo "  AstraRoot P6 — Boot Integration"
echo "=========================================="
echo ""

# -- Step 1: check device --
info "Step 1: Checking device..."
if ! adb get-state >/dev/null 2>&1; then
    fail "No device connected"
fi
pass "Device connected"

# -- Step 2: check astrad exists --
info "Step 2: Checking astrad..."
if ! adb shell "su -c 'test -f $ASTRA_DIR/astrad && echo yes'" 2>/dev/null | grep -q "yes"; then
    fail "astrad not found at $ASTRA_DIR/astrad. Run P5 first."
fi
pass "astrad exists"

# -- Step 3: check kernel module --
info "Step 3: Checking kernel module..."
KO_PATH=""
if adb shell "su -c 'test -f $ASTRA_DIR/astra_root.ko && echo yes'" 2>/dev/null | grep -q "yes"; then
    KO_PATH="$ASTRA_DIR/astra_root.ko"
elif adb shell "su -c 'test -f /data/local/tmp/astra_root.ko && echo yes'" 2>/dev/null | grep -q "yes"; then
    KO_PATH="/data/local/tmp/astra_root.ko"
fi

if [ -z "$KO_PATH" ]; then
    if [ -f "$PROJECT_DIR/kernel/astra_root.ko" ]; then
        adb push "$PROJECT_DIR/kernel/astra_root.ko" "$ASTRA_DIR/astra_root.ko"
        KO_PATH="$ASTRA_DIR/astra_root.ko"
        pass "astra_root.ko pushed"
    else
        fail "astra_root.ko not found"
    fi
else
    pass "astra_root.ko at $KO_PATH"
fi

# -- Step 4: create Magisk module (boot service) --
info "Step 4: Creating boot service module..."

adb shell "su -c 'mkdir -p $MAGISK_MODULE_DIR'"
adb shell "su -c 'cat > $MAGISK_MODULE_DIR/module.prop << EOF
id=astra_root_service
name=AstraRoot Boot Service
version=v1.0.0
versionCode=1
author=AstraVeil
description=Auto-load astra_root.ko and start astrad on boot
EOF'"
pass "module.prop created"

# service.sh (late_start service phase)
adb shell "su -c 'cat > $MAGISK_MODULE_DIR/service.sh << \"SCRIPT\"'
#!/system/bin/sh
MODDIR=\${0%/*}
ASTRA_DIR=\"/dev/astra\"
LOG_FILE=\"\$ASTRA_DIR/boot.log\"

log() {
    echo \"[\$(date)] \$1\" >> \$LOG_FILE
}

log \"AstraRoot boot service starting...\"

WAIT=0
while [ \"\$(getprop sys.boot_completed)\" != \"1\" ] && [ \$WAIT -lt 30 ]; do
    sleep 1
    WAIT=\$((WAIT + 1))
done
log \"Boot completed after \${WAIT}s\"

if [ ! -e /dev/astra_root ]; then
    if [ -f \"\$ASTRA_DIR/astra_root.ko\" ]; then
        insmod \"\$ASTRA_DIR/astra_root.ko\" 2>> \$LOG_FILE
        if [ \$? -eq 0 ]; then
            log \"astra_root.ko loaded\"
        else
            log \"ERROR: failed to load astra_root.ko\"
        fi
    else
        log \"WARNING: astra_root.ko not found\"
    fi
else
    log \"astra_root.ko already loaded\"
fi

WAIT=0
while [ ! -e /dev/astra_root ] && [ \$WAIT -lt 10 ]; do
    sleep 1
    WAIT=\$((WAIT + 1))
done

if [ -e /dev/astra_root ]; then
    chmod 0666 /dev/astra_root
    log \"/dev/astra_root ready\"
else
    log \"ERROR: /dev/astra_root not available\"
fi

if [ -f \"\$ASTRA_DIR/astrad\" ]; then
    pkill -f astrad 2>/dev/null
    sleep 1
    nohup \"\$ASTRA_DIR/astrad\" --socket \"\$ASTRA_DIR/astrad.sock\" >> \$LOG_FILE 2>&1 &
    ASTRA_PID=\$!
    log \"astrad started (pid=\$ASTRA_PID)\"

    WAIT=0
    while [ ! -S \"\$ASTRA_DIR/astrad.sock\" ] && [ \$WAIT -lt 10 ]; do
        sleep 1
        WAIT=\$((WAIT + 1))
    done

    if [ -S \"\$ASTRA_DIR/astrad.sock\" ]; then
        chmod 0666 \"\$ASTRA_DIR/astrad.sock\"
        log \"astrad socket ready\"
    else
        log \"WARNING: astrad socket not ready\"
    fi
else
    log \"WARNING: astrad not found\"
fi

log \"AstraRoot boot service complete\"
'SCRIPT'"
adb shell "su -c 'chmod 755 $MAGISK_MODULE_DIR/service.sh'"
pass "service.sh created"

# post-fs-data.sh
adb shell "su -c 'cat > $MAGISK_MODULE_DIR/post-fs-data.sh << \"SCRIPT\"
#!/system/bin/sh
mkdir -p /dev/astra
chmod 0755 /dev/astra
SCRIPT'"
adb shell "su -c 'chmod 755 $MAGISK_MODULE_DIR/post-fs-data.sh'"
pass "post-fs-data.sh created"

# -- Step 5: verify module structure --
info "Step 5: Verifying module..."
MODULE_CHECK=$(adb shell "su -c 'ls $MAGISK_MODULE_DIR/'" 2>/dev/null | tr -d '\r')
echo "  Module contents: $MODULE_CHECK"

if echo "$MODULE_CHECK" | grep -q "module.prop" && \
   echo "$MODULE_CHECK" | grep -q "service.sh"; then
    pass "Module structure valid"
else
    fail "Module structure incomplete"
fi

# -- done --
echo ""
echo "=========================================="
echo "  P6 Setup Complete"
echo "=========================================="
echo ""
echo "  Module: $MAGISK_MODULE_DIR"
echo "  astrad: $ASTRA_DIR/astrad"
echo "  KO:     $KO_PATH"
echo ""
echo "  To verify boot integration:"
echo "    1. Reboot: adb reboot"
echo "    2. Wait for boot complete"
echo "    3. Check: adb shell su -c 'pidof astrad'"
echo "    4. Check: adb shell su -c 'test -S $ASTRA_DIR/astrad.sock && echo OK'"
echo "    5. Check log: adb shell su -c 'cat $ASTRA_DIR/boot.log'"
echo ""
echo "  To remove boot service:"
echo "    adb shell su -c 'rm -rf $MAGISK_MODULE_DIR'"
echo ""
