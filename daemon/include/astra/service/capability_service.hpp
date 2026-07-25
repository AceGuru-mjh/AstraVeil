#pragma once

// astra/service/capability_service.hpp
//
// Returns a JSON snapshot of device capabilities. Intended to back the
// `GetCapability` IPC request (request type 0x01 in `main.cpp`).

#include <string>

namespace astra::service {

class CapabilityService {
public:
    CapabilityService() = default;

    /// Returns a JSON object describing device capabilities, e.g.:
    ///   {"android":"16","kernel":"6.1","selinux":"enforcing",
    ///    "root":false,"namespace":true}
    std::string get_capability_json() const;
};

}  // namespace astra::service
