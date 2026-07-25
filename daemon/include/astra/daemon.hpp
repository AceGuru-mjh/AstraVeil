#pragma once

// astra/daemon.hpp
//
// Umbrella header for the `astrad` daemon. Includes the public interface
// of every daemon subsystem so that translation units can pull them in
// with a single `#include <astra/daemon.hpp>`.

#include "astra/core/daemon_context.hpp"
#include "astra/executor/command_executor.hpp"
#include "astra/ipc/socket_server.hpp"
#include "astra/logger/logger.hpp"
#include "astra/sandbox/sandbox.hpp"
#include "astra/service/capability_service.hpp"
#include "astra/service/provider_service.hpp"
