#pragma once

#include <string>

namespace astra {

/// Phase 4 execution handler — the post-decode entry point that runs a
/// capability request through the full pipeline:
///
///   Permission (Kotlin cache) → Rust Policy → ExecutionPipeline → Provider
///
/// Phase 4 skeleton: delegates to the daemon's existing
/// [execution::ExecutionPipeline]. The permission + Rust gates are
/// already wired inside the pipeline (Phase 2.3).
class ExecutionHandler {
public:
    /// Execute @p capability for @p module. Returns true on success.
    bool execute(const std::string& module, const std::string& capability);
};

}  // namespace astra
