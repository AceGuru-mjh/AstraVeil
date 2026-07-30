#!/usr/bin/env bash
# Build astrad and package as a Magisk module zip.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NDK_PATH="${ANDROID_NDK_HOME:-${ANDROID_NDK:-}}"
CMAKE_TOOLCHAIN="${NDK_PATH}/build/cmake/android.toolchain.cmake"
BUILD_DIR="${SCRIPT_DIR}/../daemon/build-android"
OUT_DIR="${SCRIPT_DIR}/out"
ZIP_NAME="astraveil-daemon-v0.1.0-alpha.zip"

echo "=== Building astrad for arm64 ==="
cmake -S "${SCRIPT_DIR}/../daemon" -B "$BUILD_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="$CMAKE_TOOLCHAIN" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-26 \
    -DCMAKE_BUILD_TYPE=Release
cmake --build "$BUILD_DIR" -j"$(nproc)"

echo "=== Packaging Magisk module ==="
rm -rf "$OUT_DIR"
mkdir -p "$OUT_DIR/bin"
mkdir -p "$OUT_DIR/system/etc/init"

cp "$BUILD_DIR/astrad" "$OUT_DIR/bin/astrad"
cp "${SCRIPT_DIR}/module.prop" "$OUT_DIR/module.prop"
cp "${SCRIPT_DIR}/service.sh" "$OUT_DIR/service.sh"
cp "${SCRIPT_DIR}/system/etc/init/astrad.rc" "$OUT_DIR/system/etc/init/astrad.rc"

# Magisk requires these files
touch "$OUT_DIR/auto_mount"

cd "$OUT_DIR"
zip -r9 "${SCRIPT_DIR}/${ZIP_NAME}" .
echo "=== Done: ${SCRIPT_DIR}/${ZIP_NAME} ==="
echo "Install: adb push ${ZIP_NAME} /sdcard/ && Magisk Manager → Install from storage"
