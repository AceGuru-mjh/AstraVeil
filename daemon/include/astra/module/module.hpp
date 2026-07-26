#pragma once

#include <string>
#include <vector>

namespace astra::module {

/// A single permission requested by an AVM module. The [name] matches a
/// [astra::capability::Capability] string (e.g. "ROOT_ACCESS",
/// "MOUNT_NAMESPACE", "BOOT_PATCH"). The permission engine resolves the
/// name to a Capability and checks it against the device's
/// [astra::capability::CapabilityMatrix].
struct ModulePermission {
    std::string name;
};

/// Parsed contents of an AVM module's manifest.json.
///
/// Example manifest.json:
/// @code
/// {
///   "api_version": 2,
///   "id": "example.module",
///   "name": "Example Module",
///   "version": "1.0.0",
///   "author": "unknown",
///   "permissions": [ "ROOT_ACCESS", "MOUNT_NAMESPACE" ]
/// }
/// @endcode
struct ModuleManifest {
    int api_version = 0;
    std::string id;
    std::string name;
    std::string version;
    std::string author;
    std::vector<ModulePermission> permissions;
};

/// An installed AVM module: its parsed [manifest] plus the on-disk
/// [path] of the unpacked module directory.
class Module {
public:
    ModuleManifest manifest;
    std::string path;
};

}  // namespace astra::module
