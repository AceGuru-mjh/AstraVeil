#pragma once

#include <string>

namespace astra {

/// Native Linux sandbox policy — which isolation primitives to apply
/// for a given module.
///
/// Generated per-module from its [SandboxProfile] (risk-derived). A
/// low-risk module may only get mount isolation; a high-risk module
/// gets the full set (mount + pid + net + seccomp + landlock).
struct SandboxPolicy {
    bool mountNamespace = true;
    bool pidNamespace = false;
    bool networkNamespace = false;
    bool seccomp = false;
    bool landlock = false;
    bool allowNetwork = false;  // gates socket/connect/bind/etc. in seccomp
    std::string moduleId;
};

}  // namespace astra
