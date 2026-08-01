#pragma once
#include <string>
#include <vector>

namespace astra::provider {

/**
 * One real root-backend detection result.
 * @param detected   backend artifacts found on disk
 * @param available  detected AND its binary actually executes (functional check)
 * @param version    reported by the backend's own binary (if available)
 * @param source     HOW it was detected (provenance / audit P2-18)
 */
struct ProviderDetect {
    std::string id;
    std::string name;
    bool detected = false;
    bool available = false;
    std::string version;
    std::string source;
};

/**
 * Detect all root backends by REAL checks. Runs as root, so it can read
 * /data/adb/* (0700) — which the unprivileged App process CANNOT. This makes
 * the daemon's provider view more complete than the App's local detection
 * (relevant to audit P1-11 "two provider truths").
 */
std::vector<ProviderDetect> detect_all();

}  // namespace astra::provider
