#pragma once

#include <string>
#include <cstdint>

namespace astra {

constexpr char ASTRA_DEV_PATH[] = "/dev/astra_root";
constexpr char ASTRA_MAGIC = 'A';

// ioctl commands (match kernel/astra_root.c)
#define ASTRA_IOCTL_GET_ROOT     _IO(ASTRA_MAGIC, 1)
#define ASTRA_IOCTL_GET_CAPS     _IOW(ASTRA_MAGIC, 2, struct AstraCaps)
#define ASTRA_IOCTL_SET_POLICY   _IOW(ASTRA_MAGIC, 3, struct AstraPolicyEntry)
#define ASTRA_IOCTL_GET_VERSION  _IOR(ASTRA_MAGIC, 4, int)
#define ASTRA_IOCTL_MOUNT_HELPER _IOW(ASTRA_MAGIC, 5, struct AstraMountReq)

struct AstraCaps {
    uint64_t cap_effective;
    uint64_t cap_permitted;
    uint32_t target_uid;
    uint32_t target_gid;
    int32_t  result;
};

struct AstraPolicyEntry {
    uint32_t uid;
    uint64_t allowed_caps;
    uint32_t allow_full_root;
    uint32_t _pad;
};

struct AstraMountReq {
    char     source[256];
    char     target[256];
    char     fstype[64];
    uint64_t flags;
    char     data[256];
    int32_t  result;
};

/**
 * KernelBridge — bridge between astrad and astra_root.ko.
 *
 * Responsibilities:
 *   1. Open /dev/astra_root character device
 *   2. Obtain root or fine-grained capabilities via ioctl
 *   3. Sync Rust PolicyEngine policies to the kernel policy table
 *   4. Provide mount helper interface (bypass SELinux userspace limits)
 *
 * Lifecycle:
 *   astrad start -> KernelBridge::open() -> get_root() -> IPC service start
 *   astrad exit  -> KernelBridge::close()
 */
class KernelBridge {
public:
    static bool open();
    static void close();
    static bool is_available();

    // Full escalation (UID 0 + all capabilities)
    static bool get_root();

    // Fine-grained capability escalation
    static bool get_capabilities(uint64_t caps, uint32_t uid = 0);

    // Load policy into kernel policy table
    static bool load_policy(uint32_t uid, uint64_t caps, bool full_root);

    // Mount helper (kernel-mode ksys_mount)
    static bool mount(const std::string& source, const std::string& target,
                      const std::string& fstype, uint64_t flags,
                      const std::string& data = "");

    // umount helper
    static bool umount(const std::string& target, uint64_t flags = 0);

    // Get kernel module version
    static int get_version();

    // Check current privilege state
    static bool is_root();

private:
    static int fd_;
    static bool rooted_;
};

} // namespace astra
