#include "astra/security/audit_logger.hpp"

#include "astra/logger/logger.hpp"

#include <chrono>
#include <ctime>
#include <filesystem>
#include <fstream>
#include <iomanip>
#include <sstream>

namespace astra::security {

namespace {

/// ISO-8601 timestamp to the second, e.g. "2026-07-26T12:00:00".
std::string now_iso() {
    const auto now = std::chrono::system_clock::now();
    const auto t = std::chrono::system_clock::to_time_t(now);
    std::ostringstream os;
    os << std::put_time(std::gmtime(&t), "%Y-%m-%dT%H:%M:%SZ");
    return os.str();
}

}  // namespace

AuditLogger::AuditLogger(std::string log_path)
    : log_path_(std::move(log_path)) {
    if (log_path_.empty()) {
        log_path_ = "/data/astra/log/security.log";
    }
}

void AuditLogger::log(
    const std::string& module,
    const std::string& action,
    bool result
) {
    // Build the JSON line.
    std::ostringstream os;
    os << "{\"time\":\"" << now_iso() << "\""
       << ",\"module\":\"" << module << "\""
       << ",\"action\":\"" << action << "\""
       << ",\"result\":\"" << (result ? "ALLOW" : "DENY") << "\"}";

    const std::string line = os.str();

    // Mirror to the daemon logger so the entry is visible in logcat /
    // stderr even when the on-disk file is not writable.
    ALOGI("audit: %s", line.c_str());

    // Best-effort append to the on-disk log. Create the parent directory
    // if it does not exist. Failures are silent — audit logging must
    // never crash the daemon.
    std::error_code ec;
    if (auto p = std::filesystem::path(log_path_).parent_path(); !p.empty()) {
        std::filesystem::create_directories(p, ec);
    }
    std::ofstream f(log_path_, std::ios::app);
    if (f.is_open()) {
        f << line << '\n';
    }
}

}  // namespace astra::security
