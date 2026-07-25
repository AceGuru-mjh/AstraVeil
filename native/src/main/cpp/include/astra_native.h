#pragma once

// astra_native.h
//
// Public C++ surface for the AstraVeil native bridge. These helpers read
// low-level capability information from /proc and /sys and are consumed by
// `jni_bridge.cpp` (which exposes them to Kotlin via JNI).

#include <string>
#include <vector>

namespace astra {

/// Linux kernel identification parsed from `/proc/version`.
struct KernelInfo {
    std::string version;   // e.g. "6.1.55"
    std::string release;   // e.g. "android13-6.1"
    std::string compiler;  // e.g. "gcc version 12 ..."
};

/// SELinux state read from `/sys/fs/selinux/`.
struct SelinuxInfo {
    bool present;      // true if /sys/fs/selinux exists
    bool enforcing;    // true if currently enforcing
    std::string mode;  // "enforcing" | "permissive" | "disabled"
};

/// Read and parse `/proc/version`.
KernelInfo read_kernel_info();

/// Read SELinux state from `/sys/fs/selinux/`.
SelinuxInfo read_selinux_info();

/// Test whether `path` exists (using `access()`).
bool path_exists(const std::string& path);

/// True if overlay filesystem support is advertised in `/proc/filesystems`.
bool has_overlayfs();

/// True if the kernel reports `namespace` (mount namespace) support.
bool has_mount_namespace();

/// Return the list of `nodev`-tagged and regular filesystems the kernel
/// knows about, parsed from `/proc/filesystems`.
std::vector<std::string> supported_filesystems();

/// Read the entire contents of `path` into a string. Returns an empty
/// string on failure.
std::string read_file(const std::string& path);

}  // namespace astra
