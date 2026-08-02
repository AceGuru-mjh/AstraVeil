#pragma once
#include <string>
#include <vector>
#include <queue>
#include <unordered_map>
#include <mutex>
#include <cstdint>

namespace astra::ipc {

/**
 * Policy-controlled inter-module communication bus (Innovation 6).
 *
 * Modules cannot communicate directly. All messages flow through this
 * bus, which enforces:
 * 1. Sender must hold a valid "ipc_send" capability token.
 * 2. Recipient must hold a valid "ipc_receive" capability token.
 * 3. Message size capped at 64KB.
 * 4. Rate limiting: max 100 messages/second per module.
 *
 * Analogous to Android Binder IPC but with capability-based ACL.
 */
struct ModuleMessage {
    std::string sender_id;
    std::string recipient_id;
    std::string topic;
    std::string payload;
    uint64_t timestamp_ms;
    uint64_t sequence;
};

struct BusStats {
    uint64_t total_sent = 0;
    uint64_t total_delivered = 0;
    uint64_t total_rejected = 0;
    uint64_t total_rate_limited = 0;
};

class ModuleBus {
public:
    static constexpr size_t MAX_PAYLOAD_BYTES = 64 * 1024;
    static constexpr uint32_t MAX_MESSAGES_PER_SEC = 100;

    bool send(const ModuleMessage& msg,
              bool senderHasToken,
              bool recipientHasToken,
              bool recipientSubscribed);

    std::vector<ModuleMessage> poll(const std::string& moduleId,
                                     size_t maxMessages = 32);

    void subscribe(const std::string& moduleId, const std::string& topic);
    void unsubscribe(const std::string& moduleId, const std::string& topic);

    BusStats stats() const;

private:
    mutable std::mutex mutex_;
    std::unordered_map<std::string, std::queue<ModuleMessage>> queues_;
    std::unordered_map<std::string, std::vector<std::string>> subscriptions_;
    std::unordered_map<std::string, std::pair<uint64_t, uint32_t>> rate_limits_;
    std::unordered_map<std::string, uint64_t> sequences_;
    BusStats stats_;

    bool checkRateLimit(const std::string& moduleId);
    static uint64_t nowMs();
};

} // namespace astra::ipc
