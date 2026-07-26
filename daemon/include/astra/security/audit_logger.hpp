#pragma once

#include <string>

namespace astra::security {

/// Append-only audit log for every security-relevant decision the daemon
/// makes.
///
/// Each entry records the module, the action/permission requested, and
/// whether the request was allowed or denied. The log is written to
/// `/data/astra/log/security.log` (one JSON object per line) so it can be
/// tailed, archived, and inspected by the AstraUI Security panel.
///
/// Example entry:
/// @code
/// {"time":"2026-07-26T12:00:00","module":"example.avm",
///  "action":"ROOT_ACCESS","result":"ALLOW"}
/// @endcode
class AuditLogger {
public:
    /// @param log_path  Filesystem path to the audit log. Defaults to
    ///                   `/data/astra/log/security.log` when empty.
    explicit AuditLogger(std::string log_path = "");

    /// Record one authorisation decision.
    ///
    /// @param module   The module that raised the request.
    /// @param action   The permission/action name (e.g. "ROOT_ACCESS").
    /// @param result   true = ALLOW, false = DENY.
    void log(
        const std::string& module,
        const std::string& action,
        bool result
    );

private:
    std::string log_path_;
};

}  // namespace astra::security
