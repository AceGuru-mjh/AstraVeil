#include "astra/ipc/request_handler.hpp"

#include "astra/provider/provider_registry.hpp"
#include "astra/provider/root_provider.hpp"
#include "astra/capability/capability.hpp"
#include "astra/logger/logger.hpp"

#include <string>

namespace astra {

namespace {

/// Map a capability name string to the enum. Returns false on unknown.
bool resolve_capability(const std::string& name, capability::Capability& out) {
    for (const auto c : capability::all_capabilities()) {
        if (capability::capability_name(c) == name) {
            out = c;
            return true;
        }
    }
    return false;
}

}  // namespace

RequestHandler::RequestHandler(provider::ProviderRegistry& registry)
    : registry_(registry) {}

bool RequestHandler::handle(const char* data, int size) {
    /*
     * Phase 2.1-B:
     *
     * Frame format (text, pre-protobuf): "CAPABILITY:COMMAND".
     *
     * Flow:
     *   "MOUNT_NAMESPACE:mount -t tmpfs tmpfs /mnt"
     *     ↓ resolve_capability → Capability::MOUNT_NAMESPACE
     *     ↓ ProviderRegistry.resolve(cap) → RootProvider*
     *     ↓ provider->execute(command, output)
     *
     * The proto schema (ExecuteRequest { request_id, module_id,
     * capability, command }) is already in proto/astra.proto; this
     * text stub is replaced by ParseFromArray once libprotobuf is
     * linked into the CMake build.
     */
    if (size <= 0) {
        return false;
    }

    const std::string body(data, static_cast<std::size_t>(size));
    const auto sep = body.find(':');
    if (sep == std::string::npos) {
        ALOGW("RequestHandler: malformed request (no ':')");
        return false;
    }

    const std::string cap_name = body.substr(0, sep);
    const std::string command  = body.substr(sep + 1);

    capability::Capability cap;
    if (!resolve_capability(cap_name, cap)) {
        ALOGW("RequestHandler: unknown capability '%s'", cap_name.c_str());
        return false;
    }

    auto* provider = registry_.resolve(cap);
    if (!provider) {
        ALOGW("RequestHandler: no provider offers '%s'", cap_name.c_str());
        return false;
    }

    std::string output;
    const bool ok = provider->execute(command, output);
    ALOGI("RequestHandler: cap=%s provider=%s ok=%d",
          cap_name.c_str(), provider->name().c_str(), ok ? 1 : 0);
    return ok;
}

}  // namespace astra
