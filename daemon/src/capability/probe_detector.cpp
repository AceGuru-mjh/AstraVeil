#include "astra/capability/probe_detector.h"

#include <sched.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

#include <fstream>
#include <string>

namespace astra::capability {

namespace {

bool read_file_contains(const std::string& path, const std::string& needle) {
    std::ifstream f(path);
    if (!f) return false;
    std::string line;
    while (std::getline(f, line)) {
        if (line.find(needle) != std::string::npos) return true;
    }
    return false;
}

bool file_exists(const std::string& path) {
    struct stat st;
    return ::stat(path.c_str(), &st) == 0;
}

std::string read_first_line(const std::string& path) {
    std::ifstream f(path);
    std::string s;
    if (f) std::getline(f, s);
    return s;
}

/**
 * Safely test whether we can unshare(2) a namespace: fork a child, try
 * unshare, report result, child exits. Does NOT affect the daemon itself.
 */
bool test_unshare(int flag) {
    pid_t pid = ::fork();
    if (pid == 0) {
        const int r = ::unshare(flag);
        _exit(r == 0 ? 0 : 1);
    } else if (pid > 0) {
        int status = 0;
        ::waitpid(pid, &status, 0);
        return WIFEXITED(status) && WEXITSTATUS(status) == 0;
    }
    return false;   // fork failed
}

Probe probe_root() {
    const uid_t uid = ::getuid();
    return {uid == 0, "getuid()==" + std::to_string(uid)};
}

Probe probe_overlayfs() {
    const bool ok = read_file_contains("/proc/filesystems", "overlay");
    return {ok, "/proc/filesystems"};
}

Probe probe_mount_namespace() {
    const bool exists = file_exists("/proc/self/ns/mnt");
    const bool can = test_unshare(CLONE_NEWNS);
    return {exists && can, "/proc/self/ns/mnt + unshare(CLONE_NEWNS)"};
}

Probe probe_pid_namespace() {
    const bool exists = file_exists("/proc/self/ns/pid");
    const bool can = test_unshare(CLONE_NEWPID);
    return {exists && can, "/proc/self/ns/pid + unshare(CLONE_NEWPID)"};
}

Probe probe_net_namespace() {
    const bool exists = file_exists("/proc/self/ns/net");
    const bool can = test_unshare(CLONE_NEWNET);
    return {exists && can, "/proc/self/ns/net + unshare(CLONE_NEWNET)"};
}

Probe probe_selinux() {
    const bool present = file_exists("/sys/fs/selinux/enforce");
    std::string mode = "absent";
    if (present) {
        const std::string v = read_first_line("/sys/fs/selinux/enforce");
        mode = (v == "1") ? "enforcing" : "permissive";
    }
    return {present, "/sys/fs/selinux/enforce (" + mode + ")"};
}

Probe probe_system_write() {
    // Is /system mounted read-write? (read /proc/mounts, no actual write)
    bool rw = false;
    std::ifstream f("/proc/mounts");
    std::string line;
    while (std::getline(f, line)) {
        if (line.find(" /system ") != std::string::npos) {
            rw = (line.find(" rw,") != std::string::npos) ||
                 (line.find(" rw ") != std::string::npos);
            break;
        }
    }
    return {rw, "/proc/mounts /system flags"};
}

Probe probe_zygisk() {
    const bool ok = file_exists("/data/adb/modules/zygisk") ||
                    file_exists("/data/adb/magisk/zygisk");
    return {ok, "zygisk module path"};
}

}  // namespace

std::map<std::string, Probe> detect_all() {
    std::map<std::string, Probe> m;
    m["root"]            = probe_root();
    m["overlayfs"]       = probe_overlayfs();
    m["mount_namespace"] = probe_mount_namespace();
    m["pid_namespace"]   = probe_pid_namespace();
    m["net_namespace"]   = probe_net_namespace();
    m["selinux"]         = probe_selinux();
    m["system_write"]    = probe_system_write();
    m["zygisk"]          = probe_zygisk();
    // boot_patch needs boot partition access, Phase 0 doesn't probe
    // (honest annotation, audit P2-18)
    m["boot_patch"]      = {false, "not probed in Phase 0 (needs boot partition)"};
    return m;
}

}  // namespace astra::capability
