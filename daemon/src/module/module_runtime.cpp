#include "astra/module/module_runtime.hpp"

namespace astra::module {

ModuleRuntime::ModuleRuntime(
    const capability::CapabilityMatrix& matrix
) : matrix_(matrix) {}

bool ModuleRuntime::start(Module& module) {
    /*
     * 1. read manifest  — TODO(Phase 6.x): ManifestParser once .avm
     *    extraction is wired. The caller passes a Module whose
     *    manifest is already populated for now.
     *
     * 2. permission check — LIVE.
     */
    if (!checker_.check(module.manifest, matrix_)) {
        return false;
    }

    /*
     * 3. create sandbox  — TODO: Rust SandboxPolicy → daemon Sandbox.
     * 4. load module      — TODO: dlopen(module.path/runtime/arm64.so)
     *    or execute the module's script entry.
     * 5. start            — TODO: invoke the module entry symbol.
     */
    return true;
}

bool ModuleRuntime::stop(std::string /*id*/) {
    // TODO(Phase 6.x): track running modules and unload them.
    return true;
}

}  // namespace astra::module
