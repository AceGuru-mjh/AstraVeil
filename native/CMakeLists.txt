cmake_minimum_required(VERSION 3.22.1)
project(astra_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- Rust staticlib (optional, built by cargo-ndk) ----
# Gradle passes ASTRA_RUST_ROOT = <repo>/rust/target. We resolve the
# per-ABI staticlib from ANDROID_ABI so a single Gradle argument covers
# arm64-v8a and armeabi-v7a. When the artifact is absent, the build
# falls back to ASTRA_HAVE_RUST=0 (FFI stubbed).
set(ASTRA_RUST_LIB "" CACHE FILEPATH "Path to libastra_rust.a")
set(ASTRA_RUST_ROOT "" CACHE PATH "Root of Rust cross-compile output")

# Map Android ABI → Rust target triple
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(_rust_triple "aarch64-linux-android")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(_rust_triple "armv7-linux-androideabi")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(_rust_triple "x86_64-linux-android")
elseif(ANDROID_ABI STREQUAL "x86")
    set(_rust_triple "i686-linux-android")
endif()

if(NOT ASTRA_RUST_LIB AND ASTRA_RUST_ROOT AND _rust_triple)
    set(ASTRA_RUST_LIB "${ASTRA_RUST_ROOT}/${_rust_triple}/release/libastra_rust.a")
endif()

add_library(astra_native SHARED
    src/main/cpp/astra_native.cpp
    src/main/cpp/capability_probe.cpp
    src/main/cpp/su_scanner.cpp
)

target_include_directories(astra_native PRIVATE
    src/main/cpp/include
)

# Link Android log library
find_library(log-lib log)
target_link_libraries(astra_native ${log-lib})

# Link Rust staticlib if provided
if(ASTRA_RUST_LIB AND EXISTS "${ASTRA_RUST_LIB}")
    message(STATUS "Linking Rust staticlib: ${ASTRA_RUST_LIB}")
    target_link_libraries(astra_native "${ASTRA_RUST_LIB}")
    target_compile_definitions(astra_native PRIVATE ASTRA_HAVE_RUST=1)
else()
    message(STATUS "Rust staticlib not found (ABI=${ANDROID_ABI}), using Kotlin-only path")
    target_compile_definitions(astra_native PRIVATE ASTRA_HAVE_RUST=0)
endif()
