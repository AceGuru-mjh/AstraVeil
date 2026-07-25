# CMakeLists.txt — AstraVeil native JNI bridge
# Builds the shared library `libastra_native.so` that is loaded by
# `com.astraveil.nativebridge.NativeBridge`.
cmake_minimum_required(VERSION 3.22.1)

project(astra_native LANGUAGES CXX)

# ---- C++ standard -----------------------------------------------------------
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ---- Sources ----------------------------------------------------------------
set(ASTRA_NATIVE_SOURCES
    jni_bridge.cpp
    capability_native.cpp
    logger_native.cpp
)

add_library(astra_native SHARED ${ASTRA_NATIVE_SOURCES})

# ---- Includes ---------------------------------------------------------------
target_include_directories(astra_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)

# ---- Android log lib --------------------------------------------------------
find_library(log-lib log)

target_link_libraries(astra_native
    ${log-lib}
)

# ---- Rust staticlib linkage -------------------------------------------------
# The Rust security component lives in `AstraVeil/rust` and is compiled
# out-of-band with `cargo-ndk`:
#
#   cargo ndk -t arm64-v8a build --release
#
# This produces `rust/target/aarch64-linux-android/release/libastra_rust.a`.
# To wire it into the native bridge, uncomment the block below and declare the
# FFI prototypes in `astra_native.h`. The `astra_rust` crate exposes a C ABI
# under the `crate-type = ["staticlib"]` configured in `rust/Cargo.toml`.
#
# set(ASTRA_RUST_LIB
#     ${CMAKE_SOURCE_DIR}/../../rust/target/aarch64-linux-android/release/libastra_rust.a)
# if(EXISTS "${ASTRA_RUST_LIB}")
#     target_link_libraries(astra_native ${ASTRA_RUST_LIB})
#     target_compile_definitions(astra_native PRIVATE ASTRA_RUST_ENABLED=1)
#     # Rust static libs depend on pthread/log/dl on Android.
#     target_link_libraries(astra_native log dl pthread)
# else()
#     message(WARNING "libastra_rust.a not found — Rust security component will be unavailable. "
#                     "Run `cargo ndk -t arm64-v8a build --release` in AstraVeil/rust.")
# endif()
