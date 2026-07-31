#include <arpa/inet.h>
#include <cassert>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <vector>
namespace {
std::vector<uint8_t> encodeFrame(const std::vector<uint8_t>& payload) {
    std::vector<uint8_t> out(4 + payload.size());
    uint32_t net_len = htonl(static_cast<uint32_t>(payload.size()));
    std::memcpy(out.data(), &net_len, 4);
    if (!payload.empty()) std::memcpy(out.data() + 4, payload.data(), payload.size());
    return out;
}
bool decodeFrame(const std::vector<uint8_t>& wire, std::vector<uint8_t>& payload_out, uint32_t max_len = 8*1024*1024) {
    if (wire.size() < 4) return false;
    uint32_t net_len = 0; std::memcpy(&net_len, wire.data(), 4);
    uint32_t len = ntohl(net_len);
    if (len == 0 || len > max_len) return false;
    if (wire.size() < 4 + len) return false;
    payload_out.assign(wire.begin() + 4, wire.begin() + 4 + len);
    return true;
}
}
int main() {
    std::vector<uint8_t> payload = {'h','e','l','l','o'};
    auto wire = encodeFrame(payload);
    std::vector<uint8_t> decoded;
    assert(decodeFrame(wire, decoded)); assert(decoded == payload);
    std::vector<uint8_t> dummy;
    assert(!decodeFrame(encodeFrame({}), dummy));
    std::vector<uint8_t> truncated(wire.begin(), wire.begin() + 3);
    assert(!decodeFrame(truncated, dummy));
    std::vector<uint8_t> lying = {0x00,0x00,0x00,0xFF,'x'};
    assert(!decodeFrame(lying, dummy));
    std::vector<uint8_t> huge = {0xFF,0xFF,0xFF,0xFF};
    assert(!decodeFrame(huge, dummy));
    std::printf("PASS: frame codec\n"); return 0;
}
