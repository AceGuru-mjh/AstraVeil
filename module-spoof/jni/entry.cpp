// module-spoof/jni/entry.cpp
//
// AstraVeil Spoof Engine — Zygisk 模块入口
//
// 生命周期：
//   onLoad()            → 模块加载，保存 API 指针
//   preAppSpecialize()  → Zygote fork 后、应用代码执行前
//                         此时知道 packageName，读取配置，安装 hook
//   postAppSpecialize() → 应用代码开始执行，hook 已生效

#include <jni.h>
#include <string>
#include <android/log.h>

#include "zygisk.hpp"
#include "config_reader.h"
#include "property_hook.h"
#include "gl_spoof.h"

#define LOG_TAG "AstraSpoof"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

using namespace std;

class AstraSpoofModule : public zygisk::ModuleBase {
public:
    void onLoad(zygisk::Api *api, JNIEnv *env) override {
        this->api_ = api;
        this->env_ = env;
        LOGI("AstraVeil Spoof Engine loaded");
    }

    void preAppSpecialize(zygisk::AppSpecializeArgs *args) override {
        // ── 1. 获取目标包名 ──
        if (!args->nice_name) {
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }
        const char *pkg_chars = env_->GetStringUTFChars(args->nice_name, nullptr);
        string packageName(pkg_chars);
        env_->ReleaseStringUTFChars(args->nice_name, pkg_chars);

        // ── 2. 排除系统进程和自身 ──
        if (packageName == "android" ||
            packageName == "com.astraveil.app" ||
            packageName == "com.astraveil.xposed" ||
            packageName.find("com.android.") == 0) {
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        // ── 3. 读取配置（per-app 优先，global 兜底） ──
        SpoofConfig config = ConfigReader::load(packageName);

        if (!config.enabled) {
            // 无配置 → 卸载自身，零开销
            api_->setOption(zygisk::DLCLOSE_MODULE_LIBRARY);
            return;
        }

        LOGI("Spoofing [%s] as [%s]", packageName.c_str(),
             config.profile_name.c_str());

        // ── 4. 安装 Native 层 hook ──
        PropertyHook::install(config.props);

        if (!config.gl_renderer.empty()) {
            GlSpoof::install(config.gl_renderer, config.gl_vendor);
        }

        // 不设置 DLCLOSE_MODULE_LIBRARY → 模块留在进程中，hook 持续生效
    }

    void postAppSpecialize(const zygisk::AppSpecializeArgs *args) override {
        // hook 已在 preAppSpecialize 中安装，此处无需操作
    }

private:
    zygisk::Api *api_ = nullptr;
    JNIEnv *env_ = nullptr;
};

REGISTER_ZYGISK_MODULE(AstraSpoofModule)
