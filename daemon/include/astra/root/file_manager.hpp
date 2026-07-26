#pragma once

#include <string>

namespace astra::root {

/// File API modules use instead of touching the filesystem directly.
///
/// Permission flow:
/// @code
/// Module
///   ↓
/// File API
///   ↓
/// Permission Check (SecurityEngine)
///   ↓
/// Overlay Layer
///   ↓
/// Real File
/// @endcode
class FileManager {
public:
    bool read(const std::string& path, std::string& output);
    bool write(const std::string& path, const std::string& data);
    bool remove(const std::string& path);
};

}  // namespace astra::root
