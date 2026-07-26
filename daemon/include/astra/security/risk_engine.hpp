#pragma once

#include "astra/module/module.hpp"

#include <string>

namespace astra::security {

/// Computes a risk score for an AVM module from the permissions it
/// declares in its manifest.
///
/// Scoring rules (additive):
///   ROOT_ACCESS        +30
///   BOOT_PATCH         +40
///   NETWORK_ACCESS     +10
///   SYSTEM_WRITE       +20
///   SELINUX_CONTROL    +25
///   KERNEL_INTERFACE   +35
///   MOUNT              +10
///   FILESYSTEM_ACCESS  +10
///   IPC_ACCESS          +5
///
/// The score maps to a risk level:
///   0–29   LOW
///   30–59  MEDIUM
///   60–99  HIGH
///   100+   CRITICAL
///
/// The AstraUI Security panel renders this as a badge so users can spot
/// dangerous modules at a glance before granting permissions.
class RiskEngine {
public:
    /// @return The numeric risk score for @p module's declared permissions.
    int calculate(
        const astra::module::ModuleManifest& module
    ) const;

    /// @return The risk level label for @p module ("LOW"/"MEDIUM"/"HIGH"/"CRITICAL").
    std::string level(
        const astra::module::ModuleManifest& module
    ) const;

    /// @return The risk level label for a numeric @p score.
    static std::string level_for_score(int score);
};

}  // namespace astra::security
