#pragma once

#include <string>

namespace astra::root {

/// On-disk layout for a single overlay mount.
///
/// @code
/// /system            ← lowerdir (original system)
/// /data/astra/overlay/system/upper  ← upperdir (Astra modifications)
/// /data/astra/overlay/system/work   ← workdir (overlay internal)
/// /mnt/astra/system  ← merged (the merged view modules see)
/// @endcode
struct OverlayConfig {
    std::string lowerdir;   ///< original system partition
    std::string upperdir;   ///< Astra modification layer
    std::string workdir;    ///< overlay-internal work dir
    std::string merged;     ///< mount point for the merged view
};

/// Mounts and unmounts OverlayFS views of the system partitions.
///
/// The promise: AstraRoot NEVER writes to /system, /vendor, /product
/// directly. Every modification goes through an overlay upperdir, so a
/// single unmount restores the pristine system state — this is the
/// rollback primitive.
class OverlayManager {
public:
    OverlayManager();
    ~OverlayManager();

    /// Mount an overlay of @p lower onto @p cfg.merged using
    /// @p cfg.upperdir / @p cfg.workdir. Returns true on success.
    bool mount(
        const OverlayConfig& cfg
    );

    /// Convenience: mount a single partition by name ("system" /
    /// "vendor" / "product"). Builds the [OverlayConfig] from the
    /// canonical Astra layout under /data/astra/overlay.
    bool mount_partition(
        const std::string& name
    );

    /// Unmount the overlay at @p target.
    bool unmount(
        const std::string& target
    );
};

}  // namespace astra::root
