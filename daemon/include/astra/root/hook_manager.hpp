#pragma once

namespace astra::root {

/// Hook registration surface for AVM modules.
///
/// Future hook targets:
///   - system service hooks
///   - property hooks
///   - framework hooks (Zygote / SystemServer)
class HookManager {
public:
    bool registerHook();
    bool removeHook();
};

}  // namespace astra::root
