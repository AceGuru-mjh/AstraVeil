#include "astra/provider/provider_detector.h"

#include <sys/stat.h>
#include <sys/wait.h>
#include <cstdio>
#include <string>
#include <utility>
#include <vector>

namespace astra::provider {

namespace {

bool file_exists(const std::string& path) {
    struct stat st;
    return ::stat(path.c_str(), &st) == 0;
}

/**
 * Run a hardcoded shell command, capture stdout. Returns (exit_ok, output).
 * Commands are constants (never user input), so popen is safe here.
 */
std::pair<bool, std::string> run_cmd(const std::string& cmd) {
    std::string output;
    FILE* pipe = ::popen(cmd.c_str(), "r");
    if (!pipe) return {false, ""};
    char buf[256];
    while (::fgets(buf, sizeof(buf), pipe)) output += buf;
    const int status = ::pclose(pipe);
    const bool ok = WIFEXITED(status) && WEXITSTATUS(status) == 0;
    return {ok, output};
}

std::string trim(const std::string& s) {
    const size_t b = s.find_first_not_of(" \t\r\n");
    if (b == std::string::npos) return "";
    const size_t e = s.find_last_not_of(" \t\r\n");
    return s.substr(b, e - b + 1);
}

std::string first_line(const std::string& s) {
    const size_t n = s.find('\n');
    return trim(n == std::string::npos ? s : s.substr(0, n));
}

/** Check candidate paths; return (any_found, source fragment). */
std::pair<bool, std::string> check_paths(const std::vector<std::string>& paths) {
    std::vector<std::string> found;
    for (const auto& p : paths) {
        if (file_exists(p)) found.push_back(p);
    }
    std::string src;
    if (found.empty()) {
        src = "checked: ";
        for (size_t i = 0; i < paths.size(); ++i) {
            if (i) src += ", ";
            src += paths[i];
        }
        src += " (none found)";
        return {false, src};
    }
    src = "found: ";
    for (size_t i = 0; i < found.size(); ++i) {
        if (i) src += ", ";
        src += found[i];
    }
    return {true, src};
}

ProviderDetect detect_magisk() {
    ProviderDetect p;
    p.id = "magisk";
    p.name = "Magisk";
    auto [detected, src] = check_paths({
        "/data/adb/magisk/magisk",
        "/data/adb/magisk",
        "/system/bin/magisk",
    });
    p.detected = detected;
    p.source = src;
    if (detected) {
        // Functional check: the binary must actually run.
        auto [ok, out] = run_cmd(
            "magisk -v 2>/dev/null || /data/adb/magisk/magisk -v 2>/dev/null");
        const std::string ver = first_line(out);
        if (ok && !ver.empty()) {
            p.available = true;
            p.version = ver;
            p.source += "; exec 'magisk -v' -> " + ver;
        } else {
            p.source += "; 'magisk -v' failed (binary present but not functional)";
        }
    }
    return p;
}

ProviderDetect detect_kernelsu() {
    ProviderDetect p;
    p.id = "kernelsu";
    p.name = "KernelSU";
    auto [detected, src] = check_paths({
        "/data/adb/ksu/bin/ksud",
        "/data/adb/ksu",
        "/sys/module/kernelsu",
        "/proc/sys/kernel/ksu",
    });
    p.detected = detected;
    p.source = src;
    if (detected) {
        auto [ok, out] = run_cmd(
            "ksud --version 2>/dev/null || /data/adb/ksu/bin/ksud --version 2>/dev/null");
        const std::string ver = first_line(out);
        if (ok && !ver.empty()) {
            p.available = true;
            p.version = ver;
            p.source += "; exec 'ksud --version' -> " + ver;
        } else {
            p.source += "; 'ksud --version' failed (binary present but not functional)";
        }
    }
    return p;
}

ProviderDetect detect_apatch() {
    ProviderDetect p;
    p.id = "apatch";
    p.name = "APatch";
    auto [detected, src] = check_paths({
        "/data/adb/ap/bin/apd",
        "/data/adb/ap",
        "/sys/module/apatch",
    });
    p.detected = detected;
    p.source = src;
    if (detected) {
        auto [ok, out] = run_cmd(
            "apd --version 2>/dev/null || /data/adb/ap/bin/apd --version 2>/dev/null");
        const std::string ver = first_line(out);
        if (ok && !ver.empty()) {
            p.available = true;
            p.version = ver;
            p.source += "; exec 'apd --version' -> " + ver;
        } else {
            p.source += "; 'apd --version' failed (binary present but not functional)";
        }
    }
    return p;
}

ProviderDetect detect_astraroot() {
    ProviderDetect p;
    p.id = "astraroot";
    p.name = "AstraRoot";
    p.detected = false;
    p.available = false;
    // Honest: not implemented (audit P2-18 — don't claim what isn't real).
    p.source = "AstraRoot backend not implemented (Phase 1)";
    return p;
}

}  // namespace

std::vector<ProviderDetect> detect_all() {
    return {
        detect_magisk(),
        detect_kernelsu(),
        detect_apatch(),
        detect_astraroot(),
    };
}

}  // namespace astra::provider
