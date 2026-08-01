#pragma once
#include <map>
#include <string>

namespace astra::capability {

/**
 * One real capability probe result.
 * @param available  whether the capability is present
 * @param source     HOW it was determined (for provenance / audit P2-18)
 */
struct Probe {
    bool available = false;
    std::string source;
};

/**
 * Detect all capabilities by REAL probes (reading /proc, /sys, syscalls).
 * No hardcoded or inferred values — every result comes from an actual check.
 * Runs as the daemon's uid (root), so it can probe privileged state.
 */
std::map<std::string, Probe> detect_all();

}  // namespace astra::capability
