#include "astra/execution/execution_handler.hpp"

#include "astra/logger/logger.hpp"

namespace astra {

bool ExecutionHandler::execute(
    const std::string& module,
    const std::string& capability
) {
    /*
     * Phase 4: the handler is the post-decode entry point. The real
     * ExecutionPipeline + PolicyBridge wiring lives in main.cpp's IPC
     * handler (which already calls pipeline.run(capability, command)).
     * This class is the future home of the per-request context build
     * (requestId, module, risk) once the protobuf ExecuteRequest is
     * decoded end-to-end.
     */
    ALOGI("ExecutionHandler: module=%s capability=%s",
          module.c_str(), capability.c_str());
    return true;
}

}  // namespace astra
