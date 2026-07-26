#include "astra/execution/execution_pipeline.hpp"

#include "astra/provider/provider_manager.hpp"
#include "astra/security/policy_bridge.hpp"

#include "astra/logger/logger.hpp"

namespace astra::execution {

ExecutionPipeline::ExecutionPipeline(provider::ProviderManager& manager)
    : manager_(manager) {}

bool ExecutionPipeline::run(
    const std::string& capability,
    const std::string& command
) {
    /*
     * Phase 2.3:
     *
     * The Rust PolicyBridge is now the enforced security boundary.
     * Before any root operation reaches a provider, Rust must return
     * ALLOW. The Kotlin PermissionEngine is a fast-path cache; Rust is
     * the final authority.
     *
     *   ExecutionPipeline
     *       ↓
     *   PolicyBridge (Rust)
     *       ↓ ALLOW?
     *   Provider
     */
    PolicyBridge policy;
    const auto decision = policy.check();
    if (decision != PolicyResult::ALLOW) {
        ALOGW("ExecutionPipeline: policy denied capability=%s (%d)",
              capability.c_str(),
              decision == PolicyResult::DENY ? 1 : 2);
        return false;
    }

    auto* provider = manager_.current();
    if (!provider || !provider->available()) {
        ALOGW("ExecutionPipeline: no provider available for capability %s",
              capability.c_str());
        return false;
    }
    ALOGI("ExecutionPipeline: run capability=%s via %s",
          capability.c_str(), provider->name().c_str());

    std::string output;
    return provider->execute(command, output);
}

}  // namespace astra::execution
