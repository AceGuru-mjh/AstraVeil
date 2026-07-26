#include "astra/root/namespace_executor.hpp"

#include "astra/logger/logger.hpp"

namespace astra::root {

CommandResult NamespaceExecutor::execute(const std::string& command) {
    if (!enterNamespace()) {
        CommandResult r;
        r.success = false;
        r.output = "namespace unavailable";
        return r;
    }
    CommandRuntime runtime;
    return runtime.execute(command);
}

bool NamespaceExecutor::enterNamespace() {
    /*
     * Phase 8.6: the Astra namespace is the current process's (created
     * at RootRuntime::start). setns() into a separate holder process
     * lands when NamespaceContext is wired through RootRuntime.
     */
    ALOGI("NamespaceExecutor: enterNamespace (current process)");
    return true;
}

}  // namespace astra::root
