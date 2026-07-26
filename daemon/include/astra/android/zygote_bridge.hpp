#pragma once

namespace astra::android {

/// Bridge to the Zygote process for framework-level hooks.
///
/// AstraRoot does NOT patch Zygote directly. It exposes a hook
/// interface so AVM modules can register framework hooks that run in
/// the Zygote / app-process context.
class ZygoteBridge {
public:
    bool connect();
    bool registerHook();
    bool removeHook();
};

}  // namespace astra::android
