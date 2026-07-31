#include <cstdint>
#include <cstddef>
#include <vector>
#include <arpa/inet.h>

namespace {
bool decodeFrame(const uint8_t* data, size_t size, std::vector<uint8_t>& out, uint32_t max_len = 8*1024*1024) {
    if (size < 4) return false;
    uint32_t net_len = 0; __builtin_memcpy(&net_len, data, 4);
    uint32_t len = ntohl(net_len);
    if (len == 0 || len > max_len) return false;
    if (size < 4 + len) return false;
    out.assign(data + 4, data + 4 + len);
    return true;
}
}

extern "C" int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
    std::vector<uint8_t> payload;
    decodeFrame(data, size, payload);
    if (!payload.empty()) {
        std::vector<uint8_t> wire(4 + payload.size());
        uint32_t net_len = htonl(static_cast<uint32_t>(payload.size()));
        __builtin_memcpy(wire.data(), &net_len, 4);
        __builtin_memcpy(wire.data() + 4, payload.data(), payload.size());
        std::vector<uint8_t> roundtrip;
        decodeFrame(wire.data(), wire.size(), roundtrip);
        if (roundtrip != payload) __builtin_trap();
    }
    return 0;
}
