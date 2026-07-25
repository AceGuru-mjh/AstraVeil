// service/provider_service.cpp
//
// Detects the active root provider by inspecting /data/adb for the
// well-known control directories of each supported provider.

#include "astra/service/provider_service.hpp"

#include "astra/logger/logger.hpp"

#include <sys/stat.h>
#include <unistd.h>

#include <fstream>
#include <sstream>

namespace astra::service {

namespace {

bool path_exists(const std::string& path) {
    return ::access(path.c_str(), F_OK) == 0;
}

std::string read_first_line(const std::string& path) {
    std::ifstream f(path);
    std::string line;
    if (f.is_open()) std::getline(f, line);
    return line;
}

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
            default:   out += c;
        }
    }
    return out;
}

}  // namespace

std::string ProviderService::detect_provider_name() const {
    // Order matters: more specific providers first. AstraRoot is our own
    // and takes priority if installed; otherwise we prefer Magisk over
    // KernelSU over APatch based on market share.
    if (path_exists("/data/adb/astraroot")) return "astraroot";
    if (path_exists("/data/adb/magisk") && path_exists("/data/adb/magisk.db")) return "magisk";
    if (path_exists("/data/adb/ksu")) return "kernelsu";
    if (path_exists("/data/adb/ap")) return "apatch";
    return "none";
}

std::string ProviderService::detect_provider() const {
    const std::string provider = detect_provider_name();

    // `version` is "unknown" today. Each provider keeps its version in a
    // slightly different location (e.g. /data/adb/magisk/MAGISK_VERSION);
    // once we agree on a per-provider strategy we'll populate it here.
    std::string version = "unknown";

    if (provider == "magisk") {
        std::string v = read_first_line("/data/adb/magisk/MAGISK_VERSION");
        if (!v.empty()) version = v;
    } else if (provider == "kernelsu") {
        std::string v = read_first_line("/data/adb/ksu/version");
        if (!v.empty()) version = v;
    } else if (provider == "apatch") {
        std::string v = read_first_line("/data/adb/ap/version");
        if (!v.empty()) version = v;
    } else if (provider == "astraroot") {
        std::string v = read_first_line("/data/adb/astraroot/version");
        if (!v.empty()) version = v;
    }

    ALOGI("provider_service: detected provider=%s version=%s",
          provider.c_str(), version.c_str());

    std::ostringstream out;
    out << "{";
    out << "\"provider\":\"" << json_escape(provider) << "\",";
    out << "\"version\":\"" << json_escape(version) << "\"";
    out << "}";
    return out.str();
}

}  // namespace astra::service
