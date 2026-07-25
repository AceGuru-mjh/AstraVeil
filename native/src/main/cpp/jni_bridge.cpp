// jni_bridge.cpp
//
// JNI entry points for `com.astraveil.nativebridge.NativeBridge`. Each
// external function declared in the Kotlin object is implemented here using
// the `astra::` capability helpers.

#include "astra_native.h"
#include "logger_native.h"

#include <jni.h>

#include <string>
#include <vector>

namespace {

/// Convert a UTF-8 `std::string` into a `jstring` for return to Kotlin.
jstring to_jstring(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

/// Convert a `std::vector<std::string>` into a `jobjectArray` (Java
/// `String[]`).
jobjectArray to_jstring_array(JNIEnv* env, const std::vector<std::string>& vec) {
    jclass string_class = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(static_cast<jsize>(vec.size()), string_class, nullptr);
    for (jsize i = 0; i < static_cast<jsize>(vec.size()); ++i) {
        jstring s = to_jstring(env, vec[i]);
        env->SetObjectArrayElement(arr, i, s);
        env->DeleteLocalRef(s);
    }
    env->DeleteLocalRef(string_class);
    return arr;
}

}  // namespace

extern "C" {

// Called when `System.loadLibrary("astra_native")` succeeds. We use it only
// to advertise that the bridge is wired up.
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* /*reserved*/) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    LOGI("AstraVeil native bridge loaded");
    return JNI_VERSION_1_6;
}

JNIEXPORT jstring JNICALL
Java_com_astraveil_nativebridge_NativeBridge_nativeKernelVersion(JNIEnv* env, jclass /*clazz*/) {
    const auto info = astra::read_kernel_info();
    return to_jstring(env, info.version);
}

JNIEXPORT jstring JNICALL
Java_com_astraveil_nativebridge_NativeBridge_nativeSelinuxStatus(JNIEnv* env, jclass /*clazz*/) {
    const auto info = astra::read_selinux_info();
    return to_jstring(env, info.mode);
}

JNIEXPORT jboolean JNICALL
Java_com_astraveil_nativebridge_NativeBridge_nativeHasOverlayFs(JNIEnv* /*env*/, jclass /*clazz*/) {
    return astra::has_overlayfs() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_astraveil_nativebridge_NativeBridge_nativeHasMountNamespace(JNIEnv* /*env*/,
                                                                     jclass /*clazz*/) {
    return astra::has_mount_namespace() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobjectArray JNICALL
Java_com_astraveil_nativebridge_NativeBridge_nativeSupportedFilesystems(JNIEnv* env,
                                                                        jclass /*clazz*/) {
    return to_jstring_array(env, astra::supported_filesystems());
}

JNIEXPORT jboolean JNICALL
Java_com_astraveil_nativebridge_NativeBridge_nativeSuPathExists(JNIEnv* /*env*/,
                                                                jclass /*clazz*/) {
    // Common binary locations for super-user on rooted devices. The presence
    // of any of them is a strong hint that the device is rooted (or that
    // something is impersonating `su`).
    static const char* kSuPaths[] = {
        "/system/bin/su",      "/system/xbin/su",    "/sbin/su",
        "/system/sbin/su",     "/vendor/bin/su",     "/data/local/tmp/su",
        "/data/adb/magisk/su", "/data/adb/ksu/su",   "/data/adb/ap/su",
        "/system/app/Superuser.apk",
    };
    for (const char* p : kSuPaths) {
        if (astra::path_exists(p)) {
            LOGD("nativeSuPathExists: found %s", p);
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

}  // extern "C"
