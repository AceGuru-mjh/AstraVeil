#pragma once

#include <string>

namespace astra::sdk {

/// The context handed to an AVM module's `onLoad` entry point.
///
/// Developers never need to learn Kernel / SELinux / RootProvider / IPC
/// details — they call [requestPermission], [execute], [mount] and the
/// SDK forwards the request to the AstraVeil daemon over IPC.
class ModuleContext {
public:
    bool requestPermission(const std::string& permission);
    bool execute(const std::string& command);
    bool mount(const std::string& source, const std::string& target);
};

}  // namespace astra::sdk
