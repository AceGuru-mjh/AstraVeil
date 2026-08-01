#pragma once
#include <map>
#include <string>
#include <vector>

namespace astra::ipc {

/**
 * Structured execute request (audit P0-1).
 *
 * The client MUST send these fields; a raw command string is never the
 * public IPC API. Defaults are FAIL-SAFE: missing risk → 100 (max),
 * missing approved → false, so a malformed request trends toward denial.
 */
struct ExecReq {
    std::string module_id;
    std::string capability;
    unsigned int risk_level = 100;   // fail-safe: max risk if absent
    bool approved = false;           // fail-safe: not approved if absent
    std::string caller;              // calling package, for audit/policy
    std::string command;
};

struct ProviderInfo {
    std::string id;
    std::string name;
    bool detected = false;
    bool available = false;
    std::string version;
};

/**
 * Parse a structured execute request body (JSON).
 * Returns false if the body is not valid JSON or required fields are
 * missing/empty. Field names match the Kotlin client's ExecuteRequest:
 *   moduleId / capability / riskLevel / approved / caller / command
 */
bool parse_execute_request(const std::string& body, ExecReq& out);

// ── Response builders (proper escaping via nlohmann) ──

std::string make_exec_response(bool success, int exit_code,
                               const std::string& stdout_s,
                               const std::string& stderr_s);

std::string make_error_response(const std::string& error,
                                const std::string& reason);

/** Build a valid JSON array from audit log lines (fixes GetAuditLog). */
std::string make_audit_array(const std::string& raw_lines);

/** Ping response: {"status":"ok","version":...,"uptime_ms":...} */
std::string make_ping_response(const std::string& version, long long uptime_ms);

/** Capability matrix response: {"capabilities":{...},"count":N} */
std::string make_capability_response(const std::map<std::string, bool>& caps);

/** Provider list response: {"providers":[...],"count":N} */
std::string make_providers_response(const std::vector<ProviderInfo>& providers);

/** Generic key-value response: {"k1":v1,"k2":v2,...} */
std::string make_kv_response(const std::map<std::string, std::string>& kv);

}  // namespace astra::ipc
