#pragma once

#include <string>

namespace astra::recovery {

/// Snapshot + restore manager for AstraRoot system modifications.
class RollbackManager {
public:
    bool createSnapshot(const std::string& id);
    bool restore(const std::string& id);
};

}  // namespace astra::recovery
