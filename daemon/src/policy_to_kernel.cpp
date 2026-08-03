#include "kernel_bridge.h"

#include <string>
#include <vector>
#include <unordered_map>

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "AstraDaemon.PolicySync"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#else
#define LOGI(...) printf(__VA_ARGS__)
#define LOGW(...) printf(__VA_ARGS__)
#endif

namespace astra {

// Linux capability bit definitions (include/uapi/linux/capability.h)
constexpr uint64_t CAP_CHOWN_BIT            = 1ULL << 0;
constexpr uint64_t CAP_DAC_OVERRIDE_BIT     = 1ULL << 1;
constexpr uint64_t CAP_DAC_READ_SEARCH_BIT  = 1ULL << 2;
constexpr uint64_t CAP_FOWNER_BIT           = 1ULL << 3;
constexpr uint64_t CAP_FSETID_BIT           = 1ULL << 4;
constexpr uint64_t CAP_KILL_BIT             = 1ULL << 5;
constexpr uint64_t CAP_SETGID_BIT           = 1ULL << 6;
constexpr uint64_t CAP_SETUID_BIT           = 1ULL << 7;
constexpr uint64_t CAP_SETPCAP_BIT          = 1ULL << 8;
constexpr uint64_t CAP_NET_BIND_SERVICE_BIT = 1ULL << 10;
constexpr uint64_t CAP_NET_BROADCAST_BIT    = 1ULL << 11;
constexpr uint64_t CAP_NET_ADMIN_BIT        = 1ULL << 12;
constexpr uint64_t CAP_NET_RAW_BIT          = 1ULL << 13;
constexpr uint64_t CAP_IPC_LOCK_BIT         = 1ULL << 14;
constexpr uint64_t CAP_IPC_OWNER_BIT        = 1ULL << 15;
constexpr uint64_t CAP_SYS_MODULE_BIT       = 1ULL << 16;
constexpr uint64_t CAP_SYS_RAWIO_BIT        = 1ULL << 17;
constexpr uint64_t CAP_SYS_CHROOT_BIT       = 1ULL << 18;
constexpr uint64_t CAP_SYS_PTRACE_BIT       = 1ULL << 19;
constexpr uint64_t CAP_SYS_ADMIN_BIT        = 1ULL << 21;
constexpr uint64_t CAP_SYS_BOOT_BIT         = 1ULL << 22;
constexpr uint64_t CAP_SYS_NICE_BIT         = 1ULL << 23;
constexpr uint64_t CAP_SYS_RESOURCE_BIT     = 1ULL << 24;
constexpr uint64_t CAP_SYS_TIME_BIT         = 1ULL << 25;
constexpr uint64_t CAP_MKNOD_BIT            = 1ULL << 27;
constexpr uint64_t CAP_AUDIT_WRITE_BIT      = 1ULL << 29;
constexpr uint64_t CAP_AUDIT_CONTROL_BIT    = 1ULL << 30;
constexpr uint64_t CAP_SETFCAP_BIT          = 1ULL << 31;
constexpr uint64_t CAP_MAC_OVERRIDE_BIT     = 1ULL << 32;
constexpr uint64_t CAP_MAC_ADMIN_BIT        = 1ULL << 33;
constexpr uint64_t CAP_SYSLOG_BIT           = 1ULL << 34;
constexpr uint64_t CAP_WAKE_ALARM_BIT       = 1ULL << 35;
constexpr uint64_t CAP_BLOCK_SUSPEND_BIT    = 1ULL << 36;
constexpr uint64_t CAP_AUDIT_READ_BIT       = 1ULL << 37;
constexpr uint64_t CAP_PERFMON_BIT          = 1ULL << 38;
constexpr uint64_t CAP_BPF_BIT              = 1ULL << 39;
constexpr uint64_t CAP_CHECKPOINT_RESTORE_BIT = 1ULL << 40;

constexpr uint64_t ALL_CAPS = (1ULL << 41) - 1;

/**
 * AstraVeil capability name -> Linux capability bit mapping.
 *
 * Reasoning: AstraVeil's capability model is semantic ("mount.bind",
 * "su.shell"), while Linux kernel capabilities are a bitmask. This
 * translation layer is unique to AstraVeil — Magisk/KSU/APatch have
 * no such concept; their su is binary (root or not root).
 *
 * AstraVeil modules can declare "I only need mount.bind" and get just
 * CAP_SYS_ADMIN instead of full UID 0. This is the core value of
 * capability-based security.
 */
static const std::unordered_map<std::string, uint64_t> CAP_MAP = {
    // full root
    {"su.shell",       ALL_CAPS},
    {"su.full",        ALL_CAPS},

    // filesystem
    {"mount.bind",     CAP_SYS_ADMIN_BIT},
    {"mount.tmpfs",    CAP_SYS_ADMIN_BIT},
    {"mount.overlay",  CAP_SYS_ADMIN_BIT},
    {"filesystem.chown", CAP_CHOWN_BIT | CAP_FOWNER_BIT | CAP_FSETID_BIT},
    {"filesystem.dac", CAP_DAC_OVERRIDE_BIT | CAP_DAC_READ_SEARCH_BIT},

    // network
    {"network.raw",    CAP_NET_RAW_BIT},
    {"network.admin",  CAP_NET_ADMIN_BIT},
    {"network.bind",   CAP_NET_BIND_SERVICE_BIT},

    // system
    {"module.load",    CAP_SYS_MODULE_BIT},
    {"system.reboot",  CAP_SYS_BOOT_BIT},
    {"system.time",    CAP_SYS_TIME_BIT},
    {"system.nice",    CAP_SYS_NICE_BIT},
    {"system.chroot",  CAP_SYS_CHROOT_BIT},
    {"system.ptrace",  CAP_SYS_PTRACE_BIT},
    {"system.resource", CAP_SYS_RESOURCE_BIT},

    // properties (no kernel capability needed, via resetprop/setprop)
    {"property.set",   0},
    {"property.read",  0},

    // audit
    {"audit.write",    CAP_AUDIT_WRITE_BIT},
    {"audit.control",  CAP_AUDIT_CONTROL_BIT | CAP_AUDIT_READ_BIT},

    // security
    {"security.mac",   CAP_MAC_ADMIN_BIT | CAP_MAC_OVERRIDE_BIT},
    {"security.selinux", CAP_MAC_ADMIN_BIT},
};

/**
 * Convert an AstraVeil capability name to a Linux capability bitmask.
 */
uint64_t astra_cap_to_linux(const std::string& cap_name) {
    auto it = CAP_MAP.find(cap_name);
    if (it != CAP_MAP.end()) {
        return it->second;
    }
    LOGW("Unknown capability: %s", cap_name.c_str());
    return 0;
}

/**
 * Merge multiple AstraVeil capability names into a Linux capability bitmask.
 */
uint64_t astra_caps_to_linux(const std::vector<std::string>& cap_names) {
    uint64_t result = 0;
    for (const auto& name : cap_names) {
        result |= astra_cap_to_linux(name);
    }
    return result;
}

/**
 * Sync policies to the kernel at startup.
 *
 * Flow:
 *   1. astrad start -> KernelBridge::get_root() -> UID 0
 *   2. Read all su policies from Rust PolicyEngine
 *   3. Load each policy into the kernel via ioctl(ASTRA_IOCTL_SET_POLICY)
 *   4. Kernel fail-closed: UIDs without a loaded policy cannot escalate
 *
 * Reasoning: in-kernel policy enforcement is AstraVeil's security innovation.
 * Magisk's su policy lives in userspace (magiskd) and can be bypassed via
 * ptrace. AstraVeil's policy lives in kernel space (astra_root.ko's
 * policy_table); only UID-0 astrad can modify it, so normal processes
 * cannot tamper with it.
 */
void sync_policies_to_kernel() {
    if (!KernelBridge::is_available()) {
        LOGI("astra_root.ko not loaded, skipping kernel policy sync");
        return;
    }

    // Give astrad itself (UID 0) full permissions
    KernelBridge::load_policy(0, ALL_CAPS, true);

    // Dev phase: allow shell (UID 2000) full root
    KernelBridge::load_policy(2000, ALL_CAPS, true);

    // Production phase: read policies from Rust PolicyEngine
    // PolicyBridge::list_policies() -> iterate -> load_policy()
    //
    // Example: allow an app (UID 10123) only mount permission
    // KernelBridge::load_policy(10123, CAP_SYS_ADMIN_BIT, false);
    //
    // Example: allow an app (UID 10456) network.raw permission
    // KernelBridge::load_policy(10456, CAP_NET_RAW_BIT, false);
    //
    // Example: allow an app (UID 10789) full root
    // KernelBridge::load_policy(10789, ALL_CAPS, true);

    LOGI("Kernel policy sync complete");
}

} // namespace astra
