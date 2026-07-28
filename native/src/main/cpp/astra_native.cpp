#include <jni.h>
#include <android/log.h>
#include <string>
#include <fstream>
#include <sstream>
#include <vector>
#include <filesystem>

#define LOG_TAG "AstraNative"
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

/// Read a single-line sysfs/procfs file. Returns empty string on failure.
std::string read_sysfs(const char* path) {
    std::ifstream f(path);
    if (!f.is_open()) return "";
    std::string line;
    std::getline(f, line);
    return line;
}

/// Check if a file exists and is executable.
bool is_executable(const char* path) {
    return std::filesystem::exists(path) &&
           (std::filesystem::status(path).permissions() &
            std::filesystem::perms::owner_exec) != std::filesystem::perms::none;
}

/// Scan standard su binary locations.
std::vector<std::string> scan_su_paths() {
    static const char* paths[] = {
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/vendor/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/adb/magisk/magisk",
        "/data/adb/ksu/bin/ksud",
        "/data/adb/ap/bin/apd",
    };
    std::vector<std::string> found;
    for (const auto* p : paths) {
        if (std::filesystem::exists(p)) {
            found.emplace_back(p);
        }
    }
    return found;
}

} // namespace

extern "C" {

/**
 * JNI: Probe kernel version from /proc/version.
 */
JNIEXPORT jstring JNICALL
Java_com_astraveil_nativelib_NativeBridge_nativeGetKernelVersion(JNIEnv* env, jobject) {
    std::string content = read_sysfs("/proc/version");
    // Extract "Linux version X.Y.Z"
    auto pos = content.find("Linux version ");
    if (pos != std::string::npos) {
        auto start = pos + 14;
        auto end = content.find(' ', start);
        content = content.substr(start, end - start);
    }
    return env->NewStringUTF(content.c_str());
}

/**
 * JNI: Read SELinux enforce status.
 * Returns: 1=enforcing, 0=permissive, -1=disabled/unknown
 */
JNIEXPORT jint JNICALL
Java_com_astraveil_nativelib_NativeBridge_nativeGetSelinuxStatus(JNIEnv*, jobject) {
    std::string val = read_sysfs("/sys/fs/selinux/enforce");
    if (val.empty()) return -1;
    return (val == "1") ? 1 : 0;
}

/**
 * JNI: Check if overlayfs is available in /proc/filesystems.
 */
JNIEXPORT jboolean JNICALL
Java_com_astraveil_nativelib_NativeBridge_nativeHasOverlayFs(JNIEnv*, jobject) {
    std::ifstream f("/proc/filesystems");
    if (!f.is_open()) return JNI_FALSE;
    std::string line;
    while (std::getline(f, line)) {
        if (line.find("overlay") != std::string::npos) {
            return JNI_TRUE;
        }
    }
    return JNI_FALSE;
}

/**
 * JNI: Scan for su binaries and return found paths as a string array.
 */
JNIEXPORT jobjectArray JNICALL
Java_com_astraveil_nativelib_NativeBridge_nativeScanSuPaths(JNIEnv* env, jobject) {
    auto paths = scan_su_paths();
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray result = env->NewObjectArray(
        static_cast<jsize>(paths.size()), stringClass, nullptr);
    for (size_t i = 0; i < paths.size(); ++i) {
        env->SetObjectArrayElement(result, static_cast<jsize>(i),
                                   env->NewStringUTF(paths[i].c_str()));
    }
    return result;
}

/**
 * JNI: Check namespace support by reading /proc/self/ns/.
 */
JNIEXPORT jboolean JNICALL
Java_com_astraveil_nativelib_NativeBridge_nativeHasNamespace(JNIEnv* env, jobject,
                                                          jstring nsType) {
    const char* ns = env->GetStringUTFChars(nsType, nullptr);
    std::string path = std::string("/proc/self/ns/") + ns;
    env->ReleaseStringUTFChars(nsType, ns);
    return std::filesystem::exists(path) ? JNI_TRUE : JNI_FALSE;
}

/**
 * JNI: Read /proc/config.gz availability (kernel build options).
 * Returns true if the file exists (actual parsing done in Kotlin).
 */
JNIEXPORT jboolean JNICALL
Java_com_astraveil_nativelib_NativeBridge_nativeHasKernelConfig(JNIEnv*, jobject) {
    return std::filesystem::exists("/proc/config.gz") ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
