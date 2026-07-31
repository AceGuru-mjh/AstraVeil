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
    src/capability/capability.cpp
    src/capability/capability_matrix.cpp
    src/capability/capability_detector.cpp
    src/module/manifest_parser.cpp
    src/module/module_manager.cpp
    src/module/module_permission_checker.cpp
    src/module/module_runtime.cpp
    src/module/module_runner.cpp
    src/execution/execution_pipeline.cpp
    src/ipc/ipc_server.cpp
    src/ipc/request_handler.cpp
    src/provider/provider_registry.cpp
    src/security_bridge/policy_bridge.cpp
    src/sandbox/sandbox_manager.cpp
    src/sandbox/namespace_manager.cpp
    src/sandbox/mount_isolation.cpp
    src/sandbox/seccomp_manager.cpp
    src/sandbox/landlock_manager.cpp
    src/runtime/daemon_service.cpp
    src/ipc/message_router.cpp
    src/ipc/protobuf_handler.cpp
    src/execution/execution_handler.cpp
    src/recovery/recovery_manager.cpp
    src/security/permission.cpp
    src/security/permission_database.cpp
    src/security/security_engine.cpp
    src/security/audit_logger.cpp
    src/security/risk_engine.cpp
    src/security/signature_verifier.cpp
    src/provider/provider_manager.cpp
    src/provider/no_root_provider.cpp
    src/provider/magisk_provider.cpp
    src/provider/kernelsu_provider.cpp
    src/provider/apatch_provider.cpp
    src/provider/astra_root_provider.cpp
    src/root/root_runtime.cpp
    src/root/namespace_manager.cpp
    src/root/namespace_context.cpp
    src/root/overlay_manager.cpp
    src/root/overlay_config.cpp
    src/root/boot_manager.cpp
    src/root/command_runtime.cpp
    src/root/namespace_executor.cpp
    src/root/file_manager.cpp
    src/root/mount_manager.cpp
    src/root/hook_manager.cpp
    src/root/astra_root.cpp
    src/platform/android_platform.cpp
    src/boot/boot_parser.cpp
    src/boot/android_boot_provider.cpp
    src/android/property_manager.cpp
    src/android/zygote_bridge.cpp
    src/android/system_server_bridge.cpp
    src/security/selinux_manager.cpp
    src/security/policy_loader.cpp
    src/recovery/rollback_manager.cpp
    src/recovery/crash_guard.cpp
    src/update/update_manager.cpp
)

add_executable(astrad ${ASTRAD_SOURCES})

# ---- Includes ---------------------------------------------------------------
target_include_directories(astrad PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/include)

# ---- Threads ----------------------------------------------------------------
find_package(Threads REQUIRED)
target_link_libraries(astrad PRIVATE Threads::Threads)

# ---- Dynamic loader (dlopen / dlsym for module_runner) ----------------------
# module_runner.cpp uses dlopen() to load .avm native runtimes in the
# sandboxed child process. On glibc this is part of libdl (or libc on
# newer glibc); on Bionic (Android) it is always in libc.
target_link_libraries(astrad PRIVATE ${CMAKE_DL_LIBS})

# ---- Optional: libseccomp ---------------------------------------------------
# SeccompManager::apply() compiles with or without libseccomp. When the
# library is present we define ASTRA_HAVE_LIBSECCOMP so the real filter
# is applied; otherwise the method logs a warning and continues.
find_library(SECCOMP_LIB seccomp)
if(SECCOMP_LIB)
    target_link_libraries(astrad PRIVATE ${SECCOMP_LIB})
    target_compile_definitions(astrad PRIVATE ASTRA_HAVE_LIBSECCOMP=1)
    message(STATUS "Linked libseccomp: ${SECCOMP_LIB}")
else()
    message(STATUS "libseccomp not found — seccomp filter will be skipped")
endif()

# ---- Protobuf migration note -----------------------------------------------
# Today the daemon uses a simple length+type-byte framing with JSON payloads
# (see `src/ipc/socket_server.cpp` and `src/main.cpp`). The schema for the
# future protobuf migration lives in `proto/astra.proto` (authored by another
# task). When that lands, link `libprotobuf-lite` here and switch the handler
# in `main.cpp` to decode `astra.Request` instead of the manual framing.

# PR-F: daemon tests
option(ASTRA_BUILD_TESTS "Build daemon unit tests" ON)
if(ASTRA_BUILD_TESTS)
    enable_testing()
    add_executable(test_peer_auth tests/test_peer_auth.cpp)
    target_include_directories(test_peer_auth PRIVATE include)
    add_test(NAME peer_auth COMMAND test_peer_auth)

    add_executable(test_frame_codec tests/test_frame_codec.cpp)
    add_test(NAME frame_codec COMMAND test_frame_codec)
endif()
