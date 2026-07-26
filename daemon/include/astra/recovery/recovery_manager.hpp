#pragma once

namespace astra::recovery {

/// Daemon crash recovery — cleans up orphaned module processes and
/// restarts the daemon's IPC server when a crash is detected.
///
/// Phase 5.5 skeleton: [onDaemonCrash] logs + calls [cleanup];
/// [restore] re-initialises the daemon service. Real crash detection
/// (init respawn / watchdog) lands in a later sub-phase.
class RecoveryManager {
public:
    /// Called when the daemon detects it has crashed (segfault handler,
    /// watchdog timeout, ...). Cleans up + prepares for restart.
    void onDaemonCrash();

    /// Kill every running module process + destroy their sandboxes.
    void cleanup();

    /// Re-initialise the daemon service after a crash.
    void restore();
};

}  // namespace astra::recovery
