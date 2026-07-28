cmake_minimum_required(VERSION 3.22.1)
project(astra_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# ---- Rust staticlib (optional, built by cargo-ndk) ----
set(ASTRA_RUST_LIB "" CACHE FILEPATH "Path to libastra_rust.a")

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
    message(STATUS "Rust staticlib not found, using Kotlin-only path")
    target_compile_definitions(astra_native PRIVATE ASTRA_HAVE_RUST=0)
endif()
