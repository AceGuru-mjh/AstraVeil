#pragma once

#include <string>

namespace astra::android {

/// Android system property surface.
///
/// Abstracts get/set on the Android property store (ro.*, persist.*,
/// sys.*) so AstraRoot never touches __system_property_set directly
/// outside this class.
class PropertyManager {
public:
    std::string get(const std::string& key);
    bool set(const std::string& key, const std::string& value);
};

}  // namespace astra::android
