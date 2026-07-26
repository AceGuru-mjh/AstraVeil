#pragma once

#include <string>

namespace astra::sdk {

/// Permission tokens a module may request via [ModuleContext].
/// Mirrors [astra::security::Permission] on the daemon side.
namespace permission {
    constexpr const char* ROOT_ACCESS       = "ROOT_ACCESS";
    constexpr const char* SYSTEM_WRITE      = "SYSTEM_WRITE";
    constexpr const char* MOUNT             = "MOUNT";
    constexpr const char* BOOT_PATCH        = "BOOT_PATCH";
    constexpr const char* SELINUX_CONTROL   = "SELINUX_CONTROL";
    constexpr const char* KERNEL_INTERFACE  = "KERNEL_INTERFACE";
    constexpr const char* NETWORK_ACCESS    = "NETWORK_ACCESS";
    constexpr const char* FILESYSTEM_ACCESS = "FILESYSTEM_ACCESS";
    constexpr const char* IPC_ACCESS        = "IPC_ACCESS";
}  // namespace permission

}  // namespace astra::sdk
