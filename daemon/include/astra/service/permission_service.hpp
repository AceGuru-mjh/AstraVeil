#pragma once

#include <string>

namespace astra::provider { class ProviderManager; }

namespace astra::service {

/// Permission gate sitting between the IPC dispatcher and the
/// CommandExecutor. Delegates the execute decision to the active
/// [provider::RootProvider] held by [provider::ProviderManager].
///
/// Request path:
///   Socket IPC → PermissionService → ProviderManager → RootProvider
class PermissionService {
public:
    /// @param manager  The ProviderManager that owns the active root
    ///                 backend. Must outlive this service.
    explicit PermissionService(provider::ProviderManager& manager);

    /// @return true iff the active root provider is available and
    ///         permits execution of @p command.
    bool can_execute(
        const std::string& command
    ) const;

    /// Human-readable name of the active backend ("none" if no provider).
    std::string provider_name() const;

private:
    provider::ProviderManager& manager_;
};

}  // namespace astra::service
