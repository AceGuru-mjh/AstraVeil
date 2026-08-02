#include <jni.h>
#include <string>
#include <android/log.h>
#include "zygisk.hpp"
#include "config_reader.h"
#include "property_hook.h"
#include "gl_spoof.h"

#define LOG_TAG "AstraSpoof"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace std;

class AstraSpoofModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        api_ = api;
        env_ = env;
        LOGI("AstraVeil Spoof Engine loaded");
    }

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
        if (!args->nice_name) {
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }
        const char *pkg = env_->GetStringUTFChars(args->nice_name, nullptr);
        string packageName(pkg);
        env_->ReleaseStringUTFChars(args->nice_name, pkg);

        if (packageName == "android" ||
            packageName == "com.astraveil.app" ||
            packageName == "com.astraveil.xposed" ||
            packageName.find("com.android.") == 0) {
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        SpoofConfig config = ConfigReader::load(packageName);
        if (!config.enabled) {
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        LOGI("Spoofing [%s] as [%s]", packageName.c_str(),
             config.profile_name.c_str());

        PropertyHook::install(config.props);
        if (!config.gl_renderer.empty()) {
            GlSpoof::install(config.gl_renderer, config.gl_vendor);
        }
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *) override {}

private:
    zygisk::Api *api_ = nullptr;
    JNIEnv *env_ = nullptr;
};

REGISTER_ZYGISK_MODULE(AstraSpoofModule)
