# CMakeLists.txt — AstraVeil daemon (`astrad`)
#
# NOTE: this is **not** built by Gradle. The daemon is a standalone C++
# executable intended to run as a system service on-device. It is compiled
# separately on a Linux/Android host toolchain, e.g.:
#
#   cmake -S AstraVeil/daemon -B build-daemon \
#       -DCMAKE_TOOLCHAIN_FILE=$NDK/build/cmake/android.toolchain.cmake \
#       -DANDROID_ABI=arm64-v8a -DANDROID_PLATFORM=android-29
#   cmake --build build-daemon -j
#
# The resulting `astrad` binary is shipped inside the AstraVeil APK and
# extracted to `/dev/astra/astrad` at runtime.
cmake_minimum_required(VERSION 3.22.1)

project(astrad LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# ---- Sources ----------------------------------------------------------------
set(ASTRAD_SOURCES
    src/main.cpp
    src/ipc/socket_server.cpp
    src/service/capability_service.cpp
    src/service/provider_service.cpp
    src/service/permission_service.cpp
    src/executor/command_executor.cpp
    src/sandbox/sandbox.cpp
    src/logger/logger.cpp
    src/provider/provider_manager.cpp
    src/provider/no_root_provider.cpp
    src/provider/magisk_provider.cpp
    src/provider/kernelsu_provider.cpp
)

add_executable(astrad ${ASTRAD_SOURCES})

# ---- Includes ---------------------------------------------------------------
target_include_directories(astrad PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)

# ---- Threads ----------------------------------------------------------------
find_package(Threads REQUIRED)
target_link_libraries(astrad PRIVATE Threads::Threads)

# ---- Protobuf migration note -----------------------------------------------
# Today the daemon uses a simple length+type-byte framing with JSON payloads
# (see `src/ipc/socket_server.cpp` and `src/main.cpp`). The schema for the
# future protobuf migration lives in `proto/astra.proto` (authored by another
# task). When that lands, link `libprotobuf-lite` here and switch the handler
# in `main.cpp` to decode `astra.Request` instead of the manual framing.
