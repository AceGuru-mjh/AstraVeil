#include "astra/ipc/peer_auth.h"
#include <cassert>
#include <cstdio>
using astra::ipc::isAllowedUid;
int main() {
    assert(isAllowedUid(0)); assert(isAllowedUid(1000)); assert(isAllowedUid(2000));
    assert(isAllowedUid(10000)); assert(isAllowedUid(10123)); assert(isAllowedUid(19999));
    assert(!isAllowedUid(1)); assert(!isAllowedUid(1001)); assert(!isAllowedUid(2001));
    assert(!isAllowedUid(9999)); assert(!isAllowedUid(20000)); assert(!isAllowedUid(99999));
    std::printf("PASS: peer UID whitelist\n"); return 0;
}
