// service/capability_service.cpp
//
// Builds a JSON snapshot of device capabilities. The helpers here mirror
// the (very small) reads performed by the native bridge module — they are
// duplicated on purpose so the daemon can be compiled as a standalone
// binary with no dependency on libastra_native.

#include "astra/service/capability_service.hpp"

#include "astra/logger/logger.hpp"

#include <sys/stat.h>
#include <unistd.h>

#include <algorithm>
#include <cctype>
#include <cstring>
#include <fstream>
#include <sstream>

namespace astra::service {

namespace {

std::string trim(const std::string& s) {
    const auto a = s.find_first_not_of(" \t\r\n");
    if (a == std::string::npos) return {};
    const auto b = s.find_last_not_of(" \t\r\n");
    return s.substr(a, b - a + 1);
}

std::string read_file(const std::string& path) {
    std::ifstream f(path);
    if (!f.is_open()) return {};
    std::ostringstream ss;
    ss << f.rdbuf();
    return ss.str();
}

bool path_exists(const std::string& path) {
    return ::access(path.c_str(), F_OK) == 0;
}

std::string kernel_version() {
    const std::string raw = read_file("/proc/version");
    if (raw.empty()) return "unknown";
    // "Linux version 6.1.55-..."
    std::istringstream ss(raw);
    std::string tok1, tok2, tok3;
    ss >> tok1 >> tok2 >> tok3;
    if (tok3.empty()) return "unknown";
    // Strip everything from the first '-' so "6.1.55-android13-6.1" -> "6.1.55".
    const auto dash = tok3.find('-');
    return dash == std::string::npos ? tok3 : tok3.substr(0, dash);
}

std::string selinux_mode() {
    if (!path_exists("/sys/fs/selinux")) return "disabled";
    const std::string enforce = trim(read_file("/sys/fs/selinux/enforce"));
    if (enforce.empty()) return "permissive";
    return enforce == "1" ? "enforcing" : "permissive";
}

bool has_mount_namespace() {
    const std::string fs = read_file("/proc/filesystems");
    return fs.find("namespace") != std::string::npos ||
           fs.find("nsfs") != std::string::npos;
}

bool root_hint() {
    // Heuristic: presence of any common su path.
    static const char* kSuPaths[] = {
        "/system/bin/su", "/system/xbin/su", "/sbin/su",
        "/data/adb/magisk", "/data/adb/ksu", "/data/adb/ap",
    };
    for (const char* p : kSuPaths) {
        if (path_exists(p)) return true;
    }
    return false;
}

/// Best-effort Android API level via `getprop` (only if ro.build.version.sdk
/// is reflected in /system/build.prop). For now we leave "android":"unknown"
/// when /system/build.prop is unreadable.
std::string android_api_level() {
    const std::string build_prop = read_file("/system/build.prop");
    if (build_prop.empty()) return "unknown";
    // Look for `ro.build.version.sdk=NN` on a line by itself.
    std::istringstream ss(build_prop);
    std::string line;
    while (std::getline(ss, line)) {
        const auto pos = line.find("ro.build.version.sdk=");
        if (pos != std::string::npos) {
            std::string val = trim(line.substr(pos + std::strlen("ro.build.version.sdk=")));
            return val;
        }
    }
    return "unknown";
}

/// Escape a string for JSON. We hand-roll it because the daemon does not
/// currently link a JSON library — once `nlohmann/json` (or protobuf) is
/// wired in, replace this.
std::string json_escape(const std::string& s) {
    std::string out;
    out.reserve(s.size() + 4);
    for (char c : s) {
        switch (c) {
            case '"':  out += "\\\""; break;
            case '\\': out += "\\\\"; break;
            case '\n': out += "\\n"; break;
            case '\r': out += "\\r"; break;
            case '\t': out += "\\t"; break;
            default:
                if (static_cast<unsigned char>(c) < 0x20) {
                    char buf[8];
                    std::snprintf(buf, sizeof(buf), "\\u%04x", c);
                    out += buf;
                } else {
                    out += c;
                }
        }
    }
    return out;
}

}  // namespace

std::string CapabilityService::get_capability_json() const {
    const std::string android = android_api_level();
    const std::string kernel = kernel_version();
    const std::string selinux = selinux_mode();
    const bool root = root_hint();
    const bool ns = has_mount_namespace();

    std::ostringstream out;
    out << "{";
    out << "\"android\":\"" << json_escape(android) << "\",";
    out << "\"kernel\":\"" << json_escape(kernel) << "\",";
    out << "\"selinux\":\"" << json_escape(selinux) << "\",";
    out << "\"root\":" << (root ? "true" : "false") << ",";
    out << "\"namespace\":" << (ns ? "true" : "false");
    out << "}";

    ALOGD("capability_service: %s", out.str().c_str());
    return out.str();
}

}  // namespace astra::service
