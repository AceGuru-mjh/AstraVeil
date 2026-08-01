// main.cpp — entry point for `astrad`.
//
// Wires together the IPC server, capability/provider services, command
// executor, and sandbox. The IPC framing is the 4-byte big-endian length
// prefix defined in `astra/ipc/socket_server.hpp`; the first payload byte
// is a request-type discriminator and the rest is a UTF-8 JSON body.
//
// Once `proto/astra.proto` lands this dispatcher will switch to decoding
// `astra.Request` instead of the manual framing.

#include "astra/daemon.hpp"
#include "astra/service/permission_service.hpp"
#include "astra/provider/provider_manager.hpp"
#include "astra/capability/capability.hpp"
#include "astra/capability/capability_detector.hpp"
#include "astra/module/module_manager.hpp"
#include "astra/module/module_runtime.hpp"
#include "astra/security/permission.hpp"
#include "astra/security/security_engine.hpp"
#include "astra/security/audit_logger.hpp"
#include "astra/security/risk_engine.hpp"
#include "astra/security/signature_verifier.hpp"
#include "astra/security/policy_bridge.hpp"
#include "astra/ipc/json_codec.h"
#include <nlohmann/json.hpp>

#include <getopt.h>

#include <algorithm>
#include <atomic>
#include <chrono>
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>

namespace {

/// Daemon start time for uptime calculation.
static const std::chrono::steady_clock::time_point kDaemonStart =
    std::chrono::steady_clock::now();

static long long uptimeMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - kDaemonStart).count();
}

/// Escape a string for embedding inside a JSON string literal.
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

/// Request-type discriminator byte. Lives as the first payload byte.
enum class RequestType : std::uint8_t {
    GetCapability       = 0x01,
    GetProvider         = 0x02,
    Execute             = 0x03,
    Ping                = 0x04,
    GetCapabilityMatrix = 0x05,
    InstallModule       = 0x06,
    RemoveModule        = 0x07,
    StartModule         = 0x08,
    StopModule          = 0x09,
    ListModules         = 0x0A,
    QueryPermission     = 0x0B,
    GrantPermission     = 0x0C,
    RevokePermission    = 0x0D,
    GetRiskScore        = 0x0E,
    GetAuditLog         = 0x0F,
};

void print_help(const char* argv0) {
    std::fprintf(stderr,
        "astrad — AstraVeil daemon\n"
        "Usage: %s [--socket PATH] [--version] [--help]\n"
        "  --socket PATH   Unix domain socket path (default: /dev/astra/astrad.sock)\n"
        "  --version       Print version and exit\n"
        "  --help          Show this help and exit\n",
        argv0);
}

}  // namespace

int main(int argc, char** argv) {
    std::string socket_path = "/dev/astra/astrad.sock";

    static const struct option kLongOpts[] = {
        {"socket",  required_argument, nullptr, 's'},
        {"version", no_argument,       nullptr, 'v'},
        {"help",    no_argument,       nullptr, 'h'},
        {nullptr,   0,                 nullptr,  0 },
    };

    int opt;
    while ((opt = ::getopt_long(argc, argv, "s:vh", kLongOpts, nullptr)) != -1) {
        switch (opt) {
            case 's': socket_path = ::optarg ? ::optarg : ""; break;
            case 'v':
                std::printf("astrad %s\n", astra::core::DaemonContext{}.version.c_str());
                return 0;
            case 'h':
                print_help(argv[0]);
                return 0;
            default:
                print_help(argv[0]);
                return 1;
        }
    }

    astra::core::DaemonContext ctx;
    ctx.socket_path = socket_path;
    ctx.version = "0.1.0";

    ALOGI("astrad %s starting (socket=%s)", ctx.version.c_str(), ctx.socket_path.c_str());

    astra::service::CapabilityService capability_service;
    astra::service::ProviderService provider_service;
    astra::executor::CommandExecutor executor;

    astra::provider::ProviderManager provider_manager;
    provider_manager.initialize();

    astra::service::PermissionService permission_service(provider_manager);

    // P0-2: PolicyBridge — all Execute requests must pass through the Rust
    // policy engine before running. Fail-closed if Rust is not linked.
    astra::PolicyBridge policy_bridge;

    // Build the device capability matrix once at startup from the active
    // provider + independent device probes. Re-detect on GetCapabilityMatrix
    // so a provider change or SELinux flip is reflected without a restart.
    astra::capability::CapabilityDetector capability_detector;
    auto capability_matrix =
        capability_detector.detect(provider_manager.current());

    // AVM module registry + runtime. The runtime reads the live
    // capability matrix so module permission checks reflect the current
    // device + provider state.
    astra::module::ModuleManager module_manager;
    astra::module::ModuleRuntime module_runtime(capability_matrix);

    // Security framework: the authorisation decision point (SecurityEngine),
    // the audit trail (AuditLogger), the risk scorer (RiskEngine), and the
    // module signature checker (SignatureVerifier).
    astra::security::SecurityEngine security_engine(capability_matrix);
    astra::security::AuditLogger audit_logger;
    astra::security::RiskEngine risk_engine;
    astra::security::SignatureVerifier signature_verifier;

    {
        auto* rp = provider_manager.current();
        const std::string pname = rp ? rp->name() : "none";
        ALOGI("astrad: active root provider = %s", pname.c_str());
        ALOGI("astrad: capability matrix = %s", capability_matrix.json().c_str());
    }

    // Wire the IPC handler. The payload's first byte selects the service;
    // the remainder is a UTF-8 JSON body (ignored by GetCapability /
    // GetProvider / Ping, used as a command string by Execute).
    auto handler = [&](const std::vector<std::uint8_t>& req) -> std::vector<std::uint8_t> {
        if (req.empty()) {
            ALOGW("astrad: empty request");
            return {};
        }
        const auto type = static_cast<RequestType>(req[0]);
        const std::string body(req.begin() + 1, req.end());

        std::string response_json;
        switch (type) {
            case RequestType::GetCapability: {
                response_json = capability_service.get_capability_json();
                break;
            }
            case RequestType::GetProvider: {
                auto* rp = provider_manager.current();
                const std::string pname = rp ? rp->name() : "none";
                const int ptype = rp ? static_cast<int>(rp->type()) : 0;
                const bool pavail = rp ? rp->available() : false;

                // Build the capability matrix: every capability in the
                // enum is emitted with a boolean indicating whether the
                // active provider offers it. NoRoot reports none, so on
                // an unrooted device every entry is false.
                const auto caps = provider_manager.capabilities();
                std::map<std::string, bool> cap_map;
                for (const auto c : astra::capability::all_capabilities()) {
                    const bool offered =
                        std::find(caps.begin(), caps.end(), c) != caps.end();
                    cap_map[astra::capability::capability_name(c)] = offered;
                }
                // Build response with nlohmann (proper escaping)
                nlohmann::json j;
                j["root"]["provider"] = pname;
                j["root"]["type"] = ptype;
                j["root"]["available"] = pavail;
                nlohmann::json cap_json = nlohmann::json::object();
                for (const auto& [k, v] : cap_map) cap_json[k] = v;
                j["capabilities"] = cap_json;
                response_json = j.dump();

                ctx.active_provider = pname;
                ctx.provider_online.store(pname != "none");
                break;
            }
            case RequestType::Execute: {
                // P0-1: parse structured request — raw command strings are
                // never the public IPC API. Missing fields → reject.
                astra::ipc::ExecReq req;
                if (!astra::ipc::parse_execute_request(body, req)) {
                    response_json = astra::ipc::make_error_response(
                        "bad_request",
                        "malformed or incomplete execute request "
                        "(need moduleId/capability/riskLevel/approved/"
                        "caller/command)");
                    break;
                }

                if (!permission_service.can_execute(req.command)) {
                    response_json = astra::ipc::make_error_response(
                        "permission_denied", "no active root provider");
                    break;
                }

                ALOGI("Execute: caller=%s module=%s cap=%s risk=%u approved=%d",
                      req.caller.c_str(), req.module_id.c_str(),
                      req.capability.c_str(), req.risk_level,
                      req.approved ? 1 : 0);

                // P0-2: structured policy check (fail-closed if Rust not linked)
                const auto decision = policy_bridge.checkWith(
                    req.module_id, req.capability,
                    req.risk_level, req.approved);

                if (decision == astra::PolicyResult::DENY) {
                    response_json = astra::ipc::make_error_response(
                        "policy_denied",
                        "Rust policy denied execution for module '" +
                        req.module_id + "' capability '" + req.capability + "'");
                    break;
                }
                if (decision == astra::PolicyResult::APPROVAL) {
                    response_json = astra::ipc::make_error_response(
                        "approval_required",
                        "execution requires user approval");
                    break;
                }

                // Only ALLOW reaches here.
                const auto result = executor.execute(req.command);
                response_json = astra::ipc::make_exec_response(
                    result.exit_code == 0, result.exit_code,
                    result.stdout_, result.stderr_);
                break;
            }
            case RequestType::Ping: {
                response_json = astra::ipc::make_ping_response(
                    ctx.version, uptimeMs());
                break;
            }
            case RequestType::GetCapabilityMatrix: {
                // Re-probe so callers always see the live matrix rather
                // than a stale snapshot from daemon startup.
                capability_matrix =
                    capability_detector.detect(provider_manager.current());
                response_json = capability_matrix.json();
                break;
            }
            case RequestType::InstallModule: {
                // `body` is the on-disk path to the .avm package.
                const bool ok = module_manager.install(body);
                response_json = ok
                    ? "{\"installed\":true}"
                    : "{\"installed\":false,\"error\":\"install_failed\"}";
                break;
            }
            case RequestType::RemoveModule: {
                // `body` is the module id.
                const bool ok = module_manager.remove(body);
                response_json = ok
                    ? "{\"removed\":true}"
                    : "{\"removed\":false,\"error\":\"not_found\"}";
                break;
            }
            case RequestType::StartModule: {
                // `body` is the module id; find it then ask the runtime
                // to start it (manifest → permission check → sandbox → load).
                auto mods = module_manager.list();
                bool started = false;
                for (auto& m : mods) {
                    if (m.manifest.id == body) {
                        started = module_runtime.start(m);
                        break;
                    }
                }
                response_json = started
                    ? "{\"started\":true}"
                    : "{\"started\":false,\"error\":\"denied_or_not_found\"}";
                break;
            }
            case RequestType::StopModule: {
                const bool ok = module_runtime.stop(body);
                response_json = ok
                    ? "{\"stopped\":true}"
                    : "{\"stopped\":false}";
                break;
            }
            case RequestType::ListModules: {
                auto mods = module_manager.list();
                std::ostringstream mo;
                mo << "[";
                bool first = true;
                for (const auto& m : mods) {
                    if (!first) mo << ",";
                    first = false;
                    mo << "{\"id\":\"" << json_escape(m.manifest.id) << "\""
                       << ",\"name\":\"" << json_escape(m.manifest.name) << "\""
                       << ",\"version\":\"" << json_escape(m.manifest.version) << "\""
                       << ",\"path\":\"" << json_escape(m.path) << "\"}";
                }
                mo << "]";
                response_json = mo.str();
                break;
            }
            case RequestType::QueryPermission: {
                // body = "<module_id>:<permission>" — resolve and run
                // the full SecurityEngine decision tree, then audit.
                const auto sep = body.find(':');
                if (sep == std::string::npos) {
                    response_json = "{\"granted\":false,\"message\":\"bad request\"}";
                    break;
                }
                const std::string mod = body.substr(0, sep);
                const std::string perm_name = body.substr(sep + 1);
                astra::security::Permission perm;
                if (!astra::security::permission_from_name(perm_name, perm)) {
                    response_json = "{\"granted\":false,\"message\":\"unknown permission\"}";
                    break;
                }
                const bool ok = security_engine.authorize(
                    astra::security::PermissionRequest{mod, perm, ""});
                audit_logger.log(mod, perm_name, ok);
                response_json = ok
                    ? "{\"granted\":true,\"message\":\"allow\"}"
                    : "{\"granted\":false,\"message\":\"denied\"}";
                break;
            }
            case RequestType::GrantPermission: {
                // body = "<module_id>:<permission>" — user accepted the
                // AstraUI permission dialog.
                const auto sep = body.find(':');
                if (sep == std::string::npos) {
                    response_json = "{\"granted\":false}";
                    break;
                }
                const std::string mod = body.substr(0, sep);
                const std::string perm_name = body.substr(sep + 1);
                astra::security::Permission perm;
                if (!astra::security::permission_from_name(perm_name, perm)) {
                    response_json = "{\"granted\":false}";
                    break;
                }
                const bool ok = security_engine.grant(mod, perm);
                audit_logger.log(mod, "GRANT:" + perm_name, ok);
                response_json = ok ? "{\"granted\":true}" : "{\"granted\":false}";
                break;
            }
            case RequestType::RevokePermission: {
                // body = "<module_id>:<permission>"
                const auto sep = body.find(':');
                if (sep == std::string::npos) {
                    response_json = "{\"revoked\":false}";
                    break;
                }
                const std::string mod = body.substr(0, sep);
                const std::string perm_name = body.substr(sep + 1);
                astra::security::Permission perm;
                if (!astra::security::permission_from_name(perm_name, perm)) {
                    response_json = "{\"revoked\":false}";
                    break;
                }
                const bool ok = security_engine.revoke(mod, perm);
                audit_logger.log(mod, "REVOKE:" + perm_name, ok);
                response_json = ok ? "{\"revoked\":true}" : "{\"revoked\":false}";
                break;
            }
            case RequestType::GetRiskScore: {
                // body = module id — find it and score its manifest.
                auto mods = module_manager.list();
                bool found = false;
                int score = 0;
                std::string level_str = "LOW";
                for (const auto& m : mods) {
                    if (m.manifest.id == body) {
                        found = true;
                        score = risk_engine.calculate(m.manifest);
                        level_str = risk_engine.level(m.manifest);
                        break;
                    }
                }
                if (!found) {
                    response_json = "{\"error\":\"not_found\"}";
                    break;
                }
                std::ostringstream ro;
                ro << "{\"module\":\"" << json_escape(body)
                   << "\",\"risk\":" << score
                   << ",\"level\":\"" << level_str << "\"}";
                response_json = ro.str();
                break;
            }
            case RequestType::GetAuditLog: {
                // Return audit log lines as a valid JSON array (nlohmann).
                std::ifstream af("/data/astra/log/security.log");
                std::ostringstream ro;
                std::string line;
                while (std::getline(af, line)) ro << line << "\n";
                response_json = astra::ipc::make_audit_array(ro.str());
                break;
            }
            default: {
                ALOGW("astrad: unknown request type 0x%02x", static_cast<unsigned>(type));
                response_json = astra::ipc::make_error_response(
                    "unknown_request_type",
                    "unrecognized type byte");
                break;
            }
        }

        // The response echoes the request-type byte as its first byte so the
        // client can demux without inspecting the JSON. The rest is the JSON
        // body.
        std::vector<std::uint8_t> response;
        response.reserve(1 + response_json.size());
        response.push_back(static_cast<std::uint8_t>(type));
        response.insert(response.end(), response_json.begin(), response_json.end());
        return response;
    };

    astra::ipc::SocketServer server(ctx.socket_path);
    server.set_handler(handler);

    if (!server.start()) {
        ALOGE("astrad: failed to start IPC server; aborting");
        return 1;
    }

    ctx.running.store(true);
    ALOGI("astrad: entering accept loop");
    server.run();
    ctx.running.store(false);

    ALOGI("astrad: shutting down");
    // SocketServer's destructor cleans up the socket file, but be explicit.
    server.stop();
    return 0;
}
