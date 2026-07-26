#include "astra/execution/execution_pipeline.hpp"

#include "astra/provider/provider_manager.hpp"

#include "astra/logger/logger.hpp"

namespace astra::execution {

ExecutionPipeline::ExecutionPipeline(provider::ProviderManager& manager)
    : manager_(manager) {}

bool ExecutionPipeline::run(
    const std::string& capability,
    const std::string& command
) {
    /*
     * Phase 3:
     *
     * 根据 Capability 选择 Provider.
     *
     * For now we delegate to the active provider via the manager and
     * log the capability tag so the audit trail records it. Permission
     * / risk / sandbox gates are inserted here in Phase 5.
     */
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
