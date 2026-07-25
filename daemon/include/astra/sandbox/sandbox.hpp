#pragma once

// astra/sandbox/sandbox.hpp
//
// Per-process sandbox. The eventual implementation will use
// `unshare(CLONE_NEWNS)` + `mount --bind` to confine a module's view
// of the filesystem, plus an optional `CLONE_NEWNET` for network
// isolation. For now both `setup()` and `teardown()` are stubs that
// return success.

#include <string>
#include <vector>

namespace astra::sandbox {

struct Config {
    /// Chroot / overlay root for the sandboxed process.
    std::string root;
    /// Whether to allow any network access. When false, the setup will
    /// `unshare(CLONE_NEWNET)` so the sandbox has only loopback.
    bool network = false;
    /// Paths the sandboxed module may write to (bind-mounted rw).
    std::vector<std::string> write_paths;
};

class Sandbox {
public:
    explicit Sandbox(Config config);

    /// Apply the sandbox to the calling process.
    /// TODO(sandbox-impl): unshare(CLONE_NEWNS), pivot_root, bind-mount
    ///                     write_paths, drop CAP_NET_RAW if !network.
    bool setup();

    /// Undo whatever `setup()` did. Best-effort; called from the daemon's
    /// teardown path.
    bool teardown();

    const Config& config() const noexcept { return config_; }

private:
    Config config_;
};

}  // namespace astra::sandbox
