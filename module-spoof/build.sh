#!/bin/bash
set -e

NDK="${ANDROID_NDK_HOME:-$HOME/Android/Sdk/ndk/27.0.12077973}"
CMAKE="$NDK/build/cmake/android.toolchain.cmake"

git submodule update --init --recursive jni/external/Dobby

for ABI in arm64-v8a armeabi-v7a; do
    echo "═══ $ABI ═══"
    BUILD="build/$ABI"
    mkdir -p "$BUILD"
    cmake -B "$BUILD" -S jni \
        -DCMAKE_TOOLCHAIN_FILE="$CMAKE" \
        -DANDROID_ABI="$ABI" \
        -DANDROID_PLATFORM=android-26 \
        -DCMAKE_BUILD_TYPE=Release
    cmake --build "$BUILD" -j$(nproc)
    mkdir -p module/zygisk
    cp "$BUILD/libastraveil_spoof.so" "module/zygisk/$ABI.so"
done

echo "═══ Packaging ═══"
cd module
zip -r9 ../AstraVeil-Spoof-v1.0.0.zip . -x '*.DS_Store'
cd ..
echo "Done: AstraVeil-Spoof-v1.0.0.zip"
