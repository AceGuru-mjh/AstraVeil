#pragma once
#include <sys/types.h>
namespace astra::ipc {
inline bool isAllowedUid(uid_t uid) {
    constexpr uid_t UID_ROOT = 0, UID_SYSTEM = 1000, UID_SHELL = 2000, AID_APP_START = 10000, AID_APP_END = 19999;
    return uid == UID_ROOT || uid == UID_SYSTEM || uid == UID_SHELL || (uid >= AID_APP_START && uid <= AID_APP_END);
}
}  // namespace astra::ipc
