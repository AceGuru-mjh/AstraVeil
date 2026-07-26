#pragma once

namespace astra {

/// Creates Linux namespaces (mount / pid / net) via unshare(2).
///
/// Phase 3.2: [create] calls unshare with the flags requested by the
/// [SandboxPolicy]. [enter] is a stub (setns lands when the namespace
/// is held by a long-lived child).
class NamespaceManager {
public:
    /// @param mountNs  unshare CLONE_NEWNS
    /// @param pidNs    unshare CLONE_NEWPID
    /// @param netNs    unshare CLONE_NEWNET
    /// @return true on success.
    bool create(bool mountNs, bool pidNs, bool netNs);

    /// Re-enter the namespace (Phase 3.2 stub — setns future).
    bool enter();
};

}  // namespace astra
