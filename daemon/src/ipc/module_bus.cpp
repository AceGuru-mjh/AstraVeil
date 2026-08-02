#include "astra/ipc/module_bus.hpp"
#include "astra/logger/logger.hpp"

#include <algorithm>
#include <chrono>

namespace astra::ipc {

uint64_t ModuleBus::nowMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::system_clock::now().time_since_epoch()).count();
}

bool ModuleBus::send(const ModuleMessage& msg,
                      bool senderHasToken,
                      bool recipientHasToken,
                      bool recipientSubscribed) {
    std::lock_guard lock(mutex_);
    stats_.total_sent++;

    if (msg.payload.size() > MAX_PAYLOAD_BYTES) {
        ALOGW("ModuleBus: payload too large (%zu > %zu) from %s",
              msg.payload.size(), MAX_PAYLOAD_BYTES, msg.sender_id.c_str());
        stats_.total_rejected++;
        return false;
    }

    if (!senderHasToken) {
        ALOGW("ModuleBus: sender %s lacks ipc_send token", msg.sender_id.c_str());
        stats_.total_rejected++;
        return false;
    }

    if (!checkRateLimit(msg.sender_id)) {
        ALOGW("ModuleBus: rate limit exceeded for %s", msg.sender_id.c_str());
        stats_.total_rate_limited++;
        return false;
    }

    auto& seq = sequences_[msg.sender_id];
    ModuleMessage stamped = msg;
    stamped.sequence = ++seq;
    stamped.timestamp_ms = nowMs();

    if (msg.recipient_id.empty()) {
        auto it = subscriptions_.find(msg.topic);
        if (it == subscriptions_.end() || it->second.empty()) {
            stats_.total_rejected++;
            return false;
        }
        for (const auto& sub : it->second) {
            if (sub == msg.sender_id) continue;
            queues_[sub].push(stamped);
            stats_.total_delivered++;
        }
    } else {
        if (!recipientHasToken) {
            stats_.total_rejected++;
            return false;
        }
        if (!recipientSubscribed) {
            stats_.total_rejected++;
            return false;
        }
        queues_[msg.recipient_id].push(stamped);
        stats_.total_delivered++;
    }

    return true;
}

std::vector<ModuleMessage> ModuleBus::poll(const std::string& moduleId,
                                            size_t maxMessages) {
    std::lock_guard lock(mutex_);
    std::vector<ModuleMessage> result;
    auto it = queues_.find(moduleId);
    if (it == queues_.end()) return result;

    while (!it->second.empty() && result.size() < maxMessages) {
        result.push_back(std::move(it->second.front()));
        it->second.pop();
    }
    return result;
}

void ModuleBus::subscribe(const std::string& moduleId, const std::string& topic) {
    std::lock_guard lock(mutex_);
    auto& subs = subscriptions_[topic];
    if (std::find(subs.begin(), subs.end(), moduleId) == subs.end()) {
        subs.push_back(moduleId);
        ALOGI("ModuleBus: %s subscribed to '%s'", moduleId.c_str(), topic.c_str());
    }
}

void ModuleBus::unsubscribe(const std::string& moduleId, const std::string& topic) {
    std::lock_guard lock(mutex_);
    auto it = subscriptions_.find(topic);
    if (it != subscriptions_.end()) {
        auto& subs = it->second;
        subs.erase(std::remove(subs.begin(), subs.end(), moduleId), subs.end());
    }
}

BusStats ModuleBus::stats() const {
    std::lock_guard lock(mutex_);
    return stats_;
}

bool ModuleBus::checkRateLimit(const std::string& moduleId) {
    const uint64_t now = nowMs();
    auto& [windowStart, count] = rate_limits_[moduleId];
    if (now - windowStart >= 1000) {
        windowStart = now;
        count = 1;
        return true;
    }
    if (count >= MAX_MESSAGES_PER_SEC) return false;
    count++;
    return true;
}

} // namespace astra::ipc
