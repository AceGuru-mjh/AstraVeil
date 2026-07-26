#include "astra/module/module_runner.hpp"

#include "astra/sandbox/sandbox_manager.hpp"
#include "astra/logger/logger.hpp"

namespace astra::module {

bool ModuleRunner::start(const std::string& moduleId) {
    /*
     * Phase 3.1: SandboxManager now gates module launch.
     *
     * Phase 4 will add:
     *   fork + setns(Astra namespace) + drop capabilities + dlopen(module.so)
     */
    ALOGI("ModuleRunner: start %s", moduleId.c_str());

    SandboxManager sandbox;
    if (!sandbox.create(moduleId)) {
        ALOGE("ModuleRunner: sandbox create failed for %s", moduleId.c_str());
        return false;
    }
    return true;
}

bool ModuleRunner::stop(const std::string& moduleId) {
    ALOGI("ModuleRunner: stop %s", moduleId.c_str());
    SandboxManager sandbox;
    sandbox.destroy(moduleId);
    return true;
}

}  // namespace astra::module
