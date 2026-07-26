#pragma once

#include <sys/types.h>

namespace astra::root {

/// Long-lived namespace owner.
///
/// Unlike [NamespaceManager] (which calls unshare() in the current
/// process), [NamespaceContext] forks a child that holds the namespace.
/// This lets multiple commands / modules enter the same Astra namespace
/// via setns() without each having to re-create it.
///
/// Phase 8.5 skeleton: [create] forks; the child unshares CLONE_NEWNS
/// and sleeps as the namespace holder. [switchNamespace] will setns()
/// into the child's namespace (real setns lands with the procfs path
/// lookup in a follow-up).
class NamespaceContext {
public:
    NamespaceContext();
    ~NamespaceContext();

    /// Fork a child that holds a new mount namespace.
    bool create();

    /// Mark / as private inside the namespace (stop mount propagation).
    bool isolateMount();

    /// setns() into the held namespace. Phase 8.5 stub.
    bool switchNamespace();

    /// PID of the namespace-holding child, or -1 before [create].
    pid_t pid() const;

private:
    pid_t namespace_pid_ = -1;
};

}  // namespace astra::root
