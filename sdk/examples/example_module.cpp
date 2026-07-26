#include <astra/module_api.hpp>
#include <astra/permission.hpp>
#include <astra/runtime.hpp>

#include <cstdio>

/// Example AVM module entry point.
extern "C" void avm_on_load(astra::sdk::ModuleContext& ctx) {
    std::printf("example module: API level %d\n", astra::sdk::runtime::API_LEVEL);

    if (ctx.requestPermission(astra::sdk::permission::ROOT_ACCESS)) {
        std::printf("example module: ROOT_ACCESS granted\n");
        ctx.execute("id -u");
    } else {
        std::printf("example module: ROOT_ACCESS denied\n");
    }
}
