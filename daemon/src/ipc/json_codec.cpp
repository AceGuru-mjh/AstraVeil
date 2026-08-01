#include "astra/ipc/json_codec.h"

#include <nlohmann/json.hpp>
#include <sstream>

namespace astra::ipc {

using json = nlohmann::json;

bool parse_execute_request(const std::string& body, ExecReq& out) {
    try {
        const auto j = json::parse(body);
        if (!j.is_object()) return false;

        out.module_id  = j.value("moduleId",  std::string{});
        out.capability = j.value("capability", std::string{});
        out.caller     = j.value("caller",    std::string{});
        out.command    = j.value("command",   std::string{});

        // Fail-safe numeric default: clamp to [0,100], absent → 100.
        const int risk = j.value("riskLevel", 100);
        out.risk_level = static_cast<unsigned int>(
            risk < 0 ? 100 : (risk > 100 ? 100 : risk));
        out.approved = j.value("approved", false);

        // Required fields (P0-1): reject if any is missing/empty.
        if (out.command.empty())    return false;
        if (out.module_id.empty())  return false;
        if (out.capability.empty()) return false;
        if (out.caller.empty())     return false;
        return true;
    } catch (const std::exception&) {
        return false;   // malformed JSON → reject
    }
}

std::string make_exec_response(bool success, int exit_code,
                               const std::string& stdout_s,
                               const std::string& stderr_s) {
    json j;
    j["success"]   = success;
    j["exit_code"] = exit_code;
    j["stdout"]    = stdout_s;   // nlohmann escapes quotes/newline/unicode
    j["stderr"]    = stderr_s;
    return j.dump();
}

std::string make_error_response(const std::string& error,
                                const std::string& reason) {
    json j;
    j["error"]  = error;
    j["reason"] = reason;
    return j.dump();
}

std::string make_audit_array(const std::string& raw_lines) {
    json arr = json::array();
    std::istringstream iss(raw_lines);
    std::string line;
    while (std::getline(iss, line)) {
        if (line.empty()) continue;
        try {
            arr.push_back(json::parse(line));
        } catch (const std::exception&) {
            arr.push_back(line);
        }
    }
    return arr.dump();
}

std::string make_ping_response(const std::string& version, long long uptime_ms) {
    json j;
    j["status"]     = "ok";
    j["version"]    = version;
    j["uptime_ms"]  = uptime_ms;
    return j.dump();
}

std::string make_capability_response(const std::map<std::string, bool>& caps) {
    json j;
    json c = json::object();
    for (const auto& [key, val] : caps) {
        c[key] = val;
    }
    j["capabilities"] = c;
    j["count"]        = caps.size();
    return j.dump();
}

std::string make_providers_response(const std::vector<ProviderInfo>& providers) {
    json j;
    json arr = json::array();
    for (const auto& p : providers) {
        json pj;
        pj["id"]        = p.id;
        pj["name"]      = p.name;
        pj["detected"]  = p.detected;
        pj["available"] = p.available;
        pj["version"]   = p.version;
        arr.push_back(pj);
    }
    j["providers"] = arr;
    j["count"]     = providers.size();
    return j.dump();
}

std::string make_kv_response(const std::map<std::string, std::string>& kv) {
    json j;
    for (const auto& [key, val] : kv) j[key] = val;
    return j.dump();
}

}  // namespace astra::ipc
