#include "astra/capability/capability_detector.hpp"

#include <filesystem>

namespace astra::capability {

CapabilityMatrix
CapabilityDetector::detect(
    provider::RootProvider* provider
) {
    CapabilityMatrix matrix;

    /*
     * Source 1: the active root provider.
     *
     * When a provider is present and available it contributes ROOT_ACCESS
     * plus every capability it reports via capabilities(). NoRoot reports
     * none, so on an unrooted device this block is a no-op.
     */
    if (provider && provider->available()) {
        matrix.set(Capability::ROOT_ACCESS, true);
        for (auto c : provider->capabilities()) {
            matrix.set(c, true);
        }
    }

    /*
     * Source 2: independent device-side probes.
     *
     * These do not require a root backend — they describe what the
     * kernel/system itself exposes.
     */

    // SELinux: the enforce node exists iff SELinux is loaded on the
    // device. Whether it is enforcing or permissive is a separate
    // question; here we record that SELinux control is *possible*.
    matrix.set(
        Capability::SELINUX_CONTROL,
        std::filesystem::exists("/sys/fs/selinux/enforce")
    );

    // Mount namespaces: available on every Linux >= 2.6.24, which covers
    // every Android version AstraVeil targets. The /proc/self/ns/mnt
    // symlink is the canonical presence marker.
    matrix.set(
        Capability::MOUNT_NAMESPACE,
        std::filesystem::exists("/proc/self/ns/mnt")
    );

    // The AVM module runtime itself is built into this daemon, so it is
    // always available once the daemon is running. Phase 6 wires the
    // actual loader; the capability is reported now so module manifests
    // requesting MODULE_RUNTIME can be satisfied.
    matrix.set(Capability::MODULE_RUNTIME, true);

    return matrix;
}

}  // namespace astra::capability
