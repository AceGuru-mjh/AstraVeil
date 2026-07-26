#pragma once

#include <string>

namespace astra::root {

/// On-disk layout for a single overlay mount (Phase 8.5 helper).
struct OverlayConfig {
    std::string lowerdir;   ///< original system partition
    std::string upperdir;   ///< Astra modification layer
    std::string workdir;    ///< overlay-internal work dir
    std::string merged;     ///< mount point for the merged view
};

/// Build the overlay mount option string:
///   lowerdir=...,upperdir=...,workdir=...
std::string build_overlay_options(const OverlayConfig& cfg);

/// Build a canonical [OverlayConfig] for a named partition
/// ("system" / "vendor" / "product") using the Astra layout:
///   lowerdir  = /<name>
///   upperdir  = /data/astra/overlay/<name>/upper
///   workdir   = /data/astra/overlay/<name>/work
///   merged    = /mnt/astra/<name>
OverlayConfig build_overlay_config(const std::string& partition);

}  // namespace astra::root
