#pragma once

#include <string>

namespace astra::boot {

/// Android boot-image provider abstraction.
///
/// BootManager
///     ↓
/// AndroidBootProvider
///     ↓
/// boot.img / vendor_boot / init_boot
class AndroidBootProvider {
public:
    bool detect();
    bool unpack();
    bool patch();
    bool restore();

private:
    std::string boot_path_;
};

}  // namespace astra::boot
