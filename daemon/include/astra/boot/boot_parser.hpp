#pragma once

#include <string>

namespace astra::boot {

/// Parsed boot-image metadata.
struct BootInfo {
    bool valid = false;
    int android_version = 0;
    bool has_ramdisk = false;
};

/// Parses a boot / vendor_boot / init_boot image header.
///
/// Flow:
/// @code
/// boot.img
///   ↓
/// Header 解析
///   ↓
/// Ramdisk 检测
///   ↓
/// Kernel 版本检测
///   ↓
/// BootInfo
/// @endcode
class BootParser {
public:
    BootInfo parse(const std::string& path);
};

}  // namespace astra::boot
