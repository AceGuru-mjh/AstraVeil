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

#include <getopt.h>

#include <atomic>
#include <csignal>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <sstream>
#include <string>
#include <vector>

namespace {

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
    GetCapability = 0x01,
    GetProvider   = 0x02,
    Execute       = 0x03,
    Ping          = 0x04,
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
                response_json = provider_service.detect_provider();
                const std::string name = provider_service.detect_provider_name();
                ctx.active_provider = name;
                ctx.provider_online.store(name != "none");
                break;
            }
            case RequestType::Execute: {
                // `body` is the shell command to run as the daemon's uid.
                const auto result = executor.execute(body);
                std::ostringstream out;
                out << "{\"exit_code\":" << result.exit_code
                    << ",\"stdout\":\"" << json_escape(result.stdout_) << "\""
                    << ",\"stderr\":\"" << json_escape(result.stderr_) << "\"}";
                response_json = out.str();
                break;
            }
            case RequestType::Ping: {
                response_json = "{\"pong\":true,\"version\":\"" + ctx.version + "\"}";
                break;
            }
            default: {
                ALOGW("astrad: unknown request type 0x%02x", static_cast<unsigned>(type));
                response_json = "{\"error\":\"unknown_request_type\"}";
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
