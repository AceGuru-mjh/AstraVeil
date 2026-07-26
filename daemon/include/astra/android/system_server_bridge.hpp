#pragma once

namespace astra::android {

/// Bridge to SystemServer for Astra Service registration.
class SystemServerBridge {
public:
    bool connect();
    bool registerService();
};

}  // namespace astra::android
