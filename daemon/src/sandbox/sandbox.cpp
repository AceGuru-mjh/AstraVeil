// sandbox/sandbox.cpp

#include "astra/sandbox/sandbox.hpp"

#include "astra/logger/logger.hpp"

namespace astra::sandbox {

Sandbox::Sandbox(Config config) : config_(std::move(config)) {}

bool Sandbox::setup() {
    ALOGI("sandbox: setup (stub) root=%s network=%d write_paths=%zu",
          config_.root.c_str(),
          static_cast<int>(config_.network),
          config_.write_paths.size());

    // TODO(sandbox-impl): The real implementation will:
    //   1. unshare(CLONE_NEWNS | (network ? 0 : CLONE_NEWNET))
    //   2. mount("", "/", "", MS_REC | MS_PRIVATE, nullptr)
    //   3. pivot_root(config_.root, "<oldroot>")
    //   4. bind-mount each write_paths rw, everything else ro
    //   5. drop capabilities via prctl(PR_SET_KEEPCAPS, 0) + capset
    //
    // For now we return success so the daemon's plumbing can be exercised
    // without an actual sandbox in place.
    return true;
}

bool Sandbox::teardown() {
    ALOGI("sandbox: teardown (stub)");
    // TODO(sandbox-impl): umount the bind mounts we created and pivot_root
    //                     back to the original root.
    return true;
}

}  // namespace astra::sandbox
