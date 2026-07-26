#pragma once

namespace astra {

/// Marks `/` as MS_REC|MS_PRIVATE so mounts inside the sandbox namespace
/// never propagate to the host mount tree.
class MountIsolation {
public:
    bool isolate();
};

}  // namespace astra
