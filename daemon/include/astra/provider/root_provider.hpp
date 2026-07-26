#pragma once

#include "astra/capability/capability.hpp"

#include <string>
#include <vector>

namespace astra::provider {

enum class RootType {
    NONE = 0,
    MAGISK = 1,
    KERNELSU = 2,
    APATCH = 3,
    ASTRA_ROOT = 4
};

/// Abstraction over every Android root backend.
///
/// Adding a new backend is a one-file change: implement this interface,
/// create a header, and register the provider in
/// [ProviderManager::initialize]. No call site ever branches on backend
/// identity — the capability matrix and permission engine drive every
/// decision off [capabilities].
class RootProvider {
public:
    virtual ~RootProvider() = default;

    virtual RootType type() const = 0;

    virtual bool available() const = 0;

    virtual bool execute(
        const std::string& command,
        std::string& output
    ) = 0;

    virtual std::string name() const = 0;

    /// The subset of [capability::Capability] this backend actually
    /// offers when active. Used by the capability matrix (Phase 5) to
    /// decide whether a module's requested permissions can be satisfied.
    /// Implementations MUST return a stable set (callers may cache).
    virtual std::vector<capability::Capability> capabilities() const = 0;
};

}  // namespace astra::provider
