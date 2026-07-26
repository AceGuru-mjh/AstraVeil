#include "astra/module/module_manager.hpp"

namespace astra::module {

bool ModuleManager::install(
    const std::string& path
) {
    Module module;
    module.path = path;
    modules_.push_back(module);
    return true;
}

bool ModuleManager::remove(
    const std::string& id
) {
    // TODO(Phase 6.x): match against module.manifest.id once the
    // manifest parser is wired. For now the registry is path-keyed.
    for (auto it = modules_.begin(); it != modules_.end(); ++it) {
        if (it->manifest.id == id) {
            modules_.erase(it);
            return true;
        }
    }
    return false;
}

std::vector<Module> ModuleManager::list() {
    return modules_;
}

}  // namespace astra::module
