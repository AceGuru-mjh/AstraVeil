#!/bin/bash
set -e

# ═══════════════════════════════════════════════════════
#  AstraRoot P1 build script
#  Usage: ./build.sh [gki-version]
#  Example: ./build.sh android14-6.1
#           ./build.sh android13-5.15
#           ./build.sh android12-5.10
# ═══════════════════════════════════════════════════════

GKI_VERSION="${1:-android14-6.1}"
HEADERS_DIR="/opt/gki-headers/${GKI_VERSION}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=========================================="
echo "  AstraRoot P1 Build"
echo "  GKI: ${GKI_VERSION}"
echo "  Headers: ${HEADERS_DIR}"
echo "=========================================="

# -- Step 1: check headers exist --
if [ ! -d "${HEADERS_DIR}" ]; then
    echo ""
    echo "ERROR: headers not found: ${HEADERS_DIR}"
    echo ""
    echo "Download GKI headers first:"
    echo ""
    echo "Option A (KernelSU prebuilt, recommended):"
    echo "  mkdir -p /opt/gki-headers"
    echo "  cd /opt/gki-headers"
    echo "  wget https://github.com/tiann/KernelSU/releases/download/v1.0.8/kernel-headers-${GKI_VERSION}.tar.gz"
    echo "  mkdir -p ${GKI_VERSION}"
    echo "  tar xzf kernel-headers-${GKI_VERSION}.tar.gz -C ${GKI_VERSION}"
    echo ""
    echo "Option B (Android CI):"
    echo "  Visit https://ci.android.com/builds/branches/aosp_kernel-common-${GKI_VERSION}/"
    echo "  Download kernel-headers.tar.gz"
    echo "  mkdir -p ${HEADERS_DIR}"
    echo "  tar xzf kernel-headers.tar.gz -C ${HEADERS_DIR}"
    echo ""
    exit 1
fi

# -- Step 2: compile --
echo ""
echo "> Compiling..."
make -C "${HEADERS_DIR}" M="${SCRIPT_DIR}" modules \
    ARCH=arm64 \
    CROSS_COMPILE=aarch64-linux-gnu-

# -- Step 3: verify --
echo ""
if [ -f "${SCRIPT_DIR}/astra_root.ko" ]; then
    SIZE=$(stat -c%s "${SCRIPT_DIR}/astra_root.ko" 2>/dev/null || stat -f%z "${SCRIPT_DIR}/astra_root.ko")
    echo "OK: build succeeded"
    echo "   File: ${SCRIPT_DIR}/astra_root.ko"
    echo "   Size: ${SIZE} bytes"
    echo ""
    file "${SCRIPT_DIR}/astra_root.ko"
    echo ""
    echo "Next: adb push astra_root.ko /data/local/tmp/"
    echo "      adb shell su -c 'insmod /data/local/tmp/astra_root.ko'"
else
    echo "ERROR: build failed, no astra_root.ko produced"
    exit 1
fi
