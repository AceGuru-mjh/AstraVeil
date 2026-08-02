#!/bin/bash
# module-spoof/build.sh
#
# 用法: ./build.sh
# 输出: module/zygisk/arm64-v8a.so, module/zygisk/armeabi-v7a.so

set -e

NDK_PATH="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/27.0.12077973}"
CMAKE="$NDK_PATH/build/cmake/android.toolchain.cmake"

# 初始化 Dobby submodule
git submodule update --init --recursive jni/external/Dobby

for ABI in arm64-v8a armeabi-v7a; do
    echo "═══ Building $ABI ═══"
    BUILD_DIR="build/$ABI"
    mkdir -p "$BUILD_DIR"

    cmake -B "$BUILD_DIR" -S jni \
        -DCMAKE_TOOLCHAIN_FILE="$CMAKE" \
        -DANDROID_ABI="$ABI" \
        -DANDROID_PLATFORM=android-26 \
        -DCMAKE_BUILD_TYPE=Release

    cmake --build "$BUILD_DIR" -j$(nproc)

    # Zygisk 约定：so 文件名 = ABI 名
    mkdir -p module/zygisk
    cp "$BUILD_DIR/libastraveil_spoof.so" "module/zygisk/$ABI.so"
done

echo "═══ Packaging Magisk module ═══"
cd module
zip -r9 ../AstraVeil-Spoof-v1.0.0.zip . -x '*.DS_Store'
cd ..
echo "Done: AstraVeil-Spoof-v1.0.0.zip"
