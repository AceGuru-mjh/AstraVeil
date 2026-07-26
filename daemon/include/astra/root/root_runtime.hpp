#pragma once

#include <memory>

namespace astra::root {

class NamespaceManager;
class OverlayManager;
class BootManager;

/// Top-level owner of the AstraRoot runtime.
///
/// Lifecycle:
/// @code
/// prepare()  → detect capabilities, probe kernel features
///   ↓
/// start()    → create namespace, mount overlays, ready modules
///   ↓
/// stop()     → unmount overlays, tear down namespace
///   ↓
/// recover()  → rollback to last known-good snapshot on fault
/// @endcode
///
/// The runtime is held by [AstraRootProvider] and exposed to the rest of
/// the daemon via that provider. When [ready] is true the provider
/// reports [available] and the capability matrix reflects the AstraRoot
/// surface (ROOT_ACCESS, NAMESPACE_ISOLATION, OVERLAYFS, ...).
class RootRuntime {
public:
    RootRuntime();
    ~RootRuntime();

    /// Probe the kernel for the features AstraRoot needs
    /// (CLONE_NEWNS, overlay filesystem, SELinux node). Does NOT make
    /// any persistent changes. Idempotent.
    bool prepare();

    /// Bring the runtime up: create the Astra namespace, mount the
    /// system overlay, and mark the runtime [ready]. Must be called
    /// after [prepare] succeeds.
    bool start();

    /// Tear the runtime down. Safe to call from any state.
    bool stop();

    /// Rollback to the snapshot taken at [start] time. Used by the
    /// crash guard when a module faults.
    bool recover();

    /// True between a successful [start] and [stop].
    bool ready() const;

    /// Borrow the subsystem managers. Return nullptr before [start].
    NamespaceManager* namespace_manager();
    OverlayManager*   overlay_manager();
    BootManager*      boot_manager();

private:
    bool initialized_ = false;
    bool prepared_ = false;
    std::unique_ptr<NamespaceManager> ns_;
    std::unique_ptr<OverlayManager>   overlay_;
    std::unique_ptr<BootManager>      boot_;
};

}  // namespace astra::root
