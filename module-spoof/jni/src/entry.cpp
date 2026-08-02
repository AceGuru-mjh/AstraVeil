#include <jni.h>
#include <string>
#include <fstream>
#include <android/log.h>
#include "zygisk.hpp"
#include "config_reader.h"
#include "property_hook.h"
#include "gl_spoof.h"
#include "env_shield.h"
#include "json.hpp"

#define LOG_TAG "AstraSpoof"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace std;
using json = nlohmann::json;

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

        // Never hook our own processes or system framework
        if (packageName == "android" ||
            packageName == "com.astraveil.app" ||
            packageName == "com.astraveil.xposed" ||
            packageName.find("com.android.") == 0) {
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        // -- Read shield.json (Environment Shield config) --
        // Reasoning: shield.json is independent of the spoof profile config.
        // The shield must be able to run even for apps without a spoof profile.
        ShieldConfig shield;
        bool shieldEnabled = true;
        bool spoofEnabled = true;
        {
            std::ifstream sf("/data/adb/astraveil/shield.json");
            if (sf.good()) {
                try {
                    json sj = json::parse(sf);
                    shieldEnabled = sj.value("enabled", true);
                    spoofEnabled = sj.value("spoof_enabled", true);
                    shield.hide_root = sj.value("hide_root", true);
                    shield.hide_magisk = sj.value("hide_magisk", true);
                    shield.hide_xposed = sj.value("hide_xposed", true);
                    shield.hide_mounts = sj.value("hide_mounts", true);
                    shield.hide_maps = sj.value("hide_maps", true);
                    shield.hide_selinux = sj.value("hide_selinux", true);
                    shield.hide_debugger = sj.value("hide_debugger", true);
                    shield.hide_frida = sj.value("hide_frida", true);
                    shield.hide_net_unix = sj.value("hide_net_unix", true);
                    shield.momo_bypass = sj.value("shield_momo", "false") == "true";
                    shield.ruru_bypass = sj.value("shield_ruru", "false") == "true";
                    shield.chunqiu_bypass = sj.value("shield_chunqiu", "false") == "true";
                    shield.hunter_bypass = sj.value("shield_hunter", "false") == "true";
                } catch (...) {
                    // Malformed JSON — use defaults (all enabled)
                }
            }
        }

        // If both shield and spoof are disabled, nothing to do
        if (!shieldEnabled && !spoofEnabled) {
            LOGI("Shield+Spoof disabled for [%s], skipping", packageName.c_str());
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        // -- Spoof engine (property + GL hooks) --
        // Only if spoof profile is configured AND spoofEnabled
        SpoofConfig config = ConfigReader::load(packageName);
        if (spoofEnabled && config.enabled) {
            LOGI("Spoofing [%s] as [%s]", packageName.c_str(),
                 config.profile_name.c_str());
            PropertyHook::install(config.props);
            if (!config.gl_renderer.empty()) {
                GlSpoof::install(config.gl_renderer, config.gl_vendor);
            }
        }

        // -- Environment Shield (always install if enabled) --
        // Runs independently of spoof profile — hides root/Magisk/Xposed
        // traces even for apps that don't have a spoof profile configured.
        if (shieldEnabled) {
            LOGI("EnvShield installing for [%s]", packageName.c_str());
            EnvShield::install(shield);
        }
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *) override {}

private:
    zygisk::Api *api_ = nullptr;
    JNIEnv *env_ = nullptr;
};

REGISTER_ZYGISK_MODULE(AstraSpoofModule)
