#include "astra/module/module_runner.hpp"

#include "astra/sandbox/sandbox_manager.hpp"
#include "astra/logger/logger.hpp"

#ifdef __linux__
#include <unistd.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <dlfcn.h>
#include <signal.h>
#endif

#include <mutex>
#include <unordered_map>
#include <string>

namespace astra::module {

namespace {
/// Tracks the PID of every running module process so [stop] can
/// terminate the right child. Guarded by [g_mutex].
std::mutex g_mutex;
std::unordered_map<std::string, pid_t> g_running;
}  // namespace

bool ModuleRunner::start(const std::string& moduleId) {
#ifdef __linux__
    ALOGI("ModuleRunner: start %s", moduleId.c_str());

    // Refuse to double-start.
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        if (g_running.count(moduleId) > 0) {
            ALOGW("ModuleRunner: %s already running (pid=%d)",
                  moduleId.c_str(), g_running[moduleId]);
            return false;
        }
    }

    /*
     * Fork a child process. The child:
     *   1. Applies the full sandbox (namespace + seccomp + landlock).
     *   2. dlopen()s the module's .so from its install path.
     *   3. Invokes the entry symbol.
     *   4. Exits when the entry returns or on signal.
     *
     * The parent records the child PID and returns immediately so the
     * daemon IPC thread is not blocked.
     */
    pid_t child = fork();
    if (child < 0) {
        ALOGE("ModuleRunner: fork failed for %s (errno=%d)", moduleId.c_str(), errno);
        return false;
    }

    if (child == 0) {
        /*
         * ---- CHILD PROCESS ----
         * Apply the sandbox BEFORE dlopen — if isolation fails we must
         * not run untrusted code.
         */
        SandboxManager sandbox;
        if (!sandbox.create(moduleId)) {
            ALOGE("ModuleRunner: sandbox creation failed for %s, aborting", moduleId.c_str());
            _exit(127);
        }

        // The module's .so lives at
        //   /data/astra/modules/<moduleId>/lib/<arch>/module.so
        // (path convention from the .avm unpack step). For Phase 4 we
        // resolve it here; if the file is absent we exit gracefully.
        //
        // TODO(Phase 4.1): read the actual runtime path from module.json
        //   (manifest.runtime field) instead of hardcoding lib/module.so.
        const std::string soPath =
            "/data/astra/modules/" + moduleId + "/lib/module.so";

        void* handle = dlopen(soPath.c_str(), RTLD_NOW | RTLD_LOCAL);
        if (handle == nullptr) {
            ALOGE("ModuleRunner: dlopen failed for %s: %s",
                  soPath.c_str(), dlerror());
            _exit(126);
        }

        // Resolve the entry symbol. Convention: `avm_on_load`.
        using entry_fn = int (*)(void);
        auto entry = reinterpret_cast<entry_fn>(dlsym(handle, "avm_on_load"));
        if (entry == nullptr) {
            ALOGE("ModuleRunner: avm_on_load not found in %s: %s",
                  soPath.c_str(), dlerror());
            dlclose(handle);
            _exit(125);
        }

        ALOGI("ModuleRunner: invoking avm_on_load for %s", moduleId.c_str());
        int rc = entry();

        // If the entry returns, clean up and exit.
        dlclose(handle);
        _exit(rc);
    }

    /*
     * ---- PARENT PROCESS ----
     * Record the child PID so [stop] can signal it later.
     */
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_running[moduleId] = child;
    }
    ALOGI("ModuleRunner: %s launched (pid=%d)", moduleId.c_str(), child);
    return true;
#else
    ALOGW("ModuleRunner: fork+dlopen requires Linux");
    return false;
#endif
}

bool ModuleRunner::stop(const std::string& moduleId) {
#ifdef __linux__
    ALOGI("ModuleRunner: stop %s", moduleId.c_str());

    pid_t pid;
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        auto it = g_running.find(moduleId);
        if (it == g_running.end()) {
            ALOGW("ModuleRunner: %s not running", moduleId.c_str());
            return false;
        }
        pid = it->second;
    }

    // Send SIGTERM first for graceful shutdown.
    if (kill(pid, SIGTERM) != 0) {
        ALOGW("ModuleRunner: SIGTERM failed for %s (pid=%d, errno=%d)",
              moduleId.c_str(), pid, errno);
    }

    // Wait up to 3 seconds for graceful exit, then SIGKILL.
    int status = 0;
    for (int i = 0; i < 30; ++i) {
        pid_t w = waitpid(pid, &status, WNOHANG);
        if (w == pid) {
            goto reaped;
        }
        usleep(100 * 1000);  // 100ms
    }

    // Force kill.
    ALOGW("ModuleRunner: %s did not exit on SIGTERM, sending SIGKILL", moduleId.c_str());
    kill(pid, SIGKILL);
    waitpid(pid, &status, 0);

reaped:
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_running.erase(moduleId);
    }
    ALOGI("ModuleRunner: %s stopped (pid=%d, status=0x%x)", moduleId.c_str(), pid, status);

    // Destroy the sandbox (namespaces die with the process; this is a
    // cleanup hook for any future per-module state).
    SandboxManager sandbox;
    sandbox.destroy(moduleId);
    return true;
#else
    ALOGW("ModuleRunner: stop requires Linux");
    return false;
#endif
}

}  // namespace astra::module
