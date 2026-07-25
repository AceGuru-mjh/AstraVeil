// capability_native.cpp
//
// Implementations of the `astra::` capability helpers declared in
// `astra_native.h`. These run inside the AstraVeil Android process and read
// low-level kernel/SELinux state via POSIX and procfs.

#include "astra_native.h"
#include "logger_native.h"

#include <algorithm>
#include <cstring>

#include <unistd.h>
#include <sys/stat.h>

#include <fstream>
#include <sstream>

namespace astra {

namespace {

/// Trim leading/trailing whitespace from a token in-place.
std::string trim(const std::string& in) {
    const auto begin = in.find_first_not_of(" \t\r\n");
    if (begin == std::string::npos) return {};
    const auto end = in.find_last_not_of(" \t\r\n");
    return in.substr(begin, end - begin + 1);
}

/// Split `s` on `sep` returning at most `max_parts` (0 = unlimited) tokens.
std::vector<std::string> split(const std::string& s, char sep, size_t max_parts = 0) {
    std::vector<std::string> out;
    std::stringstream ss(s);
    std::string item;
    while (std::getline(ss, item, sep)) {
        if (max_parts && out.size() + 1 == max_parts) {
            // Remaining content (including any further separators) becomes the
            // final token.
            std::string rest;
            std::getline(ss, rest, '\0');
            out.push_back(item + rest);
            return out;
        }
        out.push_back(item);
    }
    return out;
}

}  // namespace

std::string read_file(const std::string& path) {
    std::ifstream f(path, std::ios::in | std::ios::binary);
    if (!f.is_open()) {
        return {};
    }
    std::ostringstream ss;
    ss << f.rdbuf();
    return ss.str();
}

bool path_exists(const std::string& path) {
    return ::access(path.c_str(), F_OK) == 0;
}

KernelInfo read_kernel_info() {
    KernelInfo info{};
    const std::string raw = read_file("/proc/version");
    if (raw.empty()) {
        LOGW("read_kernel_info: /proc/version unreadable");
        return info;
    }

    // /proc/version format:
    //   Linux version 6.1.55-android13-6.1 (user@host) (gcc version ...) #1 SMP PREEMPT ...
    // We pull tokens 2 (version) and 3 (release-ish). Compiler string is the
    // remainder within the first set of parentheses.
    const auto tokens = split(raw, ' ', 4);
    if (tokens.size() >= 2) {
        info.version = trim(tokens[2]);
    }
    if (tokens.size() >= 3) {
        info.release = trim(tokens[3]);
    }

    const auto lp = raw.find('(');
    const auto rp = raw.find(')');
    if (lp != std::string::npos && rp != std::string::npos && rp > lp) {
        info.compiler = trim(raw.substr(lp + 1, rp - lp - 1));
    }

    LOGD("read_kernel_info: version=%s release=%s", info.version.c_str(), info.release.c_str());
    return info;
}

SelinuxInfo read_selinux_info() {
    SelinuxInfo info{};
    info.present = path_exists("/sys/fs/selinux");
    if (!info.present) {
        info.mode = "disabled";
        LOGD("read_selinux_info: selinuxfs not present");
        return info;
    }

    // /sys/fs/selinux/enforce contains a single ASCII digit: '1' enforcing,
    // '0' permissive.
    const std::string enforce_raw = read_file("/sys/fs/selinux/enforce");
    if (enforce_raw.empty()) {
        info.enforcing = false;
        info.mode = "permissive";
    } else {
        info.enforcing = trim(enforce_raw) == "1";
        info.mode = info.enforcing ? "enforcing" : "permissive";
    }
    LOGD("read_selinux_info: mode=%s", info.mode.c_str());
    return info;
}

bool has_overlayfs() {
    const auto fs_list = supported_filesystems();
    return std::any_of(fs_list.begin(), fs_list.end(),
                       [](const std::string& fs) { return fs == "overlay" || fs == "overlayfs"; });
}

bool has_mount_namespace() {
    const auto fs_list = supported_filesystems();
    // Mount-namespace support is reflected by the kernel advertising the
    // `namespace` pseudo filesystem. Also accept `nsfs` (newer kernels).
    return std::any_of(fs_list.begin(), fs_list.end(), [](const std::string& fs) {
        return fs == "namespace" || fs == "nsfs";
    });
}

std::vector<std::string> supported_filesystems() {
    std::vector<std::string> out;
    std::ifstream f("/proc/filesystems");
    if (!f.is_open()) {
        LOGW("supported_filesystems: /proc/filesystems unreadable");
        return out;
    }
    std::string line;
    while (std::getline(f, line)) {
        // Each line is either `nodev\t<fs>` or `<fs>`.
        const auto tab = line.find('\t');
        std::string fs = (tab == std::string::npos) ? line : line.substr(tab + 1);
        fs = trim(fs);
        if (!fs.empty()) {
            out.push_back(fs);
        }
    }
    return out;
}

}  // namespace astra
