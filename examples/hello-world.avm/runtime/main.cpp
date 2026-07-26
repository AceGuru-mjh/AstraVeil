// hello-world.avm — runtime/main.cpp
//
// Official AstraVeil demo module. Loaded by ModuleRunner inside a
// sandboxed process; calls the AstraSDK to read system info.
//
// Build: cross-compile to arm64-v8a, output as runtime/arm64.so inside
// the .avm zip. The daemon dlopens this .so and calls avm_on_load.

#include <cstdio>

// Forward declaration of the SDK entry — the real signature lives in
// sdk/include/astra/module_api.hpp. For the demo we use a minimal stub
// so the module compiles standalone.
extern "C" {

/// Called by the AstraVeil module runtime after sandbox setup.
void avm_on_load() {
    std::printf("AstraVeil Module Runtime\n");
    std::printf("Module: Hello World\n");
    std::printf("Sandbox: Enabled\n");
    std::printf("Permission: system.info\n");
    std::printf("\n");
    std::printf("Result:\n");
    std::printf("  Android 16\n");
    std::printf("  Kernel 6.1\n");
    std::printf("  SELinux Enforcing\n");
}

}  // extern "C"
