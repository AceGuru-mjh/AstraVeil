#include "astra/module/module_runner.hpp"

#include "astra/logger/logger.hpp"

namespace astra::module {

bool ModuleRunner::start(const std::string& moduleId) {
    /*
     * Phase 4 实现隔离进程:
     *   fork + setns(Astra namespace) + drop capabilities + dlopen(module.so)
     *
     * 当前只建立接口.
     */
    ALOGI("ModuleRunner: start %s (stub — Phase 4 isolation)", moduleId.c_str());
    return true;
}

bool ModuleRunner::stop(const std::string& moduleId) {
    ALOGI("ModuleRunner: stop %s (stub)", moduleId.c_str());
    return true;
}

}  // namespace astra::module
