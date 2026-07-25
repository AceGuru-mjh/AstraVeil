// executor/command_executor.cpp

#include "astra/executor/command_executor.hpp"

#include "astra/logger/logger.hpp"

#include <cstdio>
#include <cstdlib>
#include <cstring>

namespace astra::executor {

namespace {

/// Read everything from `stream` into `out`.
void drain(FILE* stream, std::string& out) {
    if (stream == nullptr) return;
    char buf[4096];
    while (true) {
        const std::size_t n = std::fread(buf, 1, sizeof(buf), stream);
        if (n == 0) break;
        out.append(buf, n);
        if (n < sizeof(buf)) break;
    }
}

}  // namespace

ExecResult CommandExecutor::execute(const std::string& cmd) const {
    ExecResult result;
    // 2>&1 so stderr is captured alongside stdout. We lose the ability to
    // separate them, but that's fine for the diagnostics path — `execute_as_root`
    // will use proper separation when it lands.
    std::string wrapped = cmd + " 2>&1";
    FILE* pipe = ::popen(wrapped.c_str(), "r");
    if (pipe == nullptr) {
        ALOGE("command_executor: popen failed for: %s", cmd.c_str());
        result.exit_code = -1;
        return result;
    }
    drain(pipe, result.stdout_);
    const int raw = ::pclose(pipe);
    // `pclose` returns a wait(2)-style status; extract the exit code.
    if (WIFEXITED(raw)) {
        result.exit_code = WEXITSTATUS(raw);
    } else if (WIFSIGNALED(raw)) {
        result.exit_code = 128 + WTERMSIG(raw);
    } else {
        result.exit_code = -1;
    }
    ALOGD("command_executor: exit=%d cmd=%s", result.exit_code, cmd.c_str());
    return result;
}

ExecResult CommandExecutor::execute_as_root(const std::string& cmd) const {
    // TODO(provider-integration): once DaemonContext::provider_online is true,
    // dispatch based on `active_provider`:
    //   magisk    -> "magisk su -c '<cmd>'"
    //   kernelsu  -> "ksud su -c '<cmd>'"
    //   apatch    -> "apd su -c '<cmd>'"
    //   astraroot -> our own AstraRoot provider IPC
    ExecResult result;
    result.exit_code = -1;
    result.stderr_ = "execute_as_root not yet implemented (no provider wired up)";
    ALOGW("command_executor: execute_as_root stub called: %s", cmd.c_str());
    return result;
}

}  // namespace astra::executor
