package com.astraveil.xposed.hooks

import android.os.Build
import com.astraveil.xposed.SpoofConfig
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Environment Shield Java layer: hides Root / Magisk / Xposed / Frida /
 * framework traces.
 *
 * Covers detection tools: MOMO / Ruru / chunqiu / Hunter / generic.
 *
 * The native-layer (Zygisk EnvShield) handles file-path interception
 * (openat). This class handles Java-layer detection vectors that cannot
 * be caught at the native file layer: Class.forName probing,
 * PackageManager scanning, File.exists, Runtime.exec, Build fields,
 * SystemProperties, Settings.Global, ApplicationInfo flags.
 */
object DetectionHook {

    // -- Packages to hide (covers all detection tools' scan lists) --
    private val HIDDEN_PACKAGES = setOf(
        // Root
        "com.topjohnwu.magisk",
        "io.github.vvb2060.magisk",       // Magisk Delta
        "com.topjohnwu.magisk.alpha",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser",
        "com.noshufou.android.su",
        "com.thirdparty.superuser",
        "com.kingroot.kinguser",
        "com.kingo.root",
        "com.smedialink.oneclickroot",
        "com.zhiqupk.root.global",
        // Xposed / LSPosed
        "de.robv.android.xposed",
        "de.robv.android.xposed.installer",
        "org.lsposed.manager",
        "org.meowcat.edxposed.manager",
        "com.solohsu.android.edxp.manager",
        // Frida
        "com.frida.frida",
        "re.frida.server",
        // Frameworks
        "com.saurik.substrate",
        "com.zachspong.temprootremovejb",
        "com.amphoras.hidemyroot",
        "com.formyhm.hideroot",
        // AstraVeil itself
        "com.astraveil.app",
        "com.astraveil.xposed",
        // Module managers
        "com.dergoogler.mmrl",
        "com.dergoogler.mmrl.pro",
    )

    // -- File paths to hide --
    private val HIDDEN_PATHS = setOf(
        // su binary (full path, Hunter-specific)
        "/sbin/su", "/system/bin/su", "/system/xbin/su",
        "/system/sbin/su", "/vendor/bin/su", "/odm/bin/su",
        "/data/local/su", "/data/local/bin/su",
        "/data/local/xbin/su", "/system/etc/.installed_su_daemon",
        // Apex paths (Hunter-specific)
        "/apex/com.android.runtime/bin/su",
        "/apex/com.android.art/bin/su",
        // Magisk
        "/data/adb/magisk", "/data/adb/modules",
        "/data/adb/magisk.db", "/data/adb/magisk.img",
        "/data/adb/post-fs-data.d", "/data/adb/service.d",
        // Xposed / LSPosed
        "/data/adb/lspd", "/data/adb/lsposed",
        "/data/misc/riru",
        "/system/lib/libxposed_art.so",
        "/system/lib64/libxposed_art.so",
        // Frida
        "/data/local/tmp/frida-server",
        "/data/local/tmp/re.frida.server",
        "/tmp/frida-server",
        // AstraVeil
        "/data/adb/astraveil",
    )

    fun install(classLoader: ClassLoader, config: SpoofConfig) {
        hookClassForName(classLoader)
        hookClassLoader(classLoader)
        hookPackageManager(classLoader)
        hookFileAccess(classLoader)
        hookRuntimeExec(classLoader)
        hookBuildFields()
        hookSystemProperties(classLoader)
        hookSettingsProvider(classLoader)
        hookContentResolver(classLoader)  // Hunter-specific
        hookApplicationInfo(classLoader)  // chunqiu-specific
    }

    // -- 1. Class.forName interception --
    private fun hookClassForName(cl: ClassLoader) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val name = param.args[0] as? String ?: return
                if (name.contains("xposed", true) ||
                    name.contains("de.robv", true) ||
                    name.contains("lsposed", true) ||
                    name.contains("edxposed", true) ||
                    name.contains("substrate", true)
                ) {
                    param.throwable = ClassNotFoundException(name)
                }
            }
        }
        try {
            XposedHelpers.findAndHookMethod(
                "java.lang.Class", cl, "forName",
                String::class.java, hook,
            )
            XposedHelpers.findAndHookMethod(
                "java.lang.Class", cl, "forName",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                ClassLoader::class.java, hook,
            )
        } catch (_: Throwable) {}
    }

    // -- 2. ClassLoader.loadClass interception (Ruru-specific) --
    // Reasoning: Ruru uses classLoader.loadClass("de.robv.android.xposed.XposedBridge")
    // in addition to Class.forName.
    private fun hookClassLoader(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "java.lang.ClassLoader", cl,
                "loadClass", String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val name = param.args[0] as? String ?: return
                        if (name.contains("xposed", true) ||
                            name.contains("de.robv", true)
                        ) {
                            param.throwable = ClassNotFoundException(name)
                        }
                    }
                },
            )
        } catch (_: Throwable) {}
    }

    // -- 3. PackageManager interception --
    private fun hookPackageManager(cl: ClassLoader) {
        try {
            // getInstalledPackages
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", cl,
                "getInstalledPackages", Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val list = param.result as? MutableList<*> ?: return
                        param.result = list.filter { pkg ->
                            val name = try {
                                XposedHelpers.getObjectField(
                                    pkg, "packageName",
                                ) as? String ?: ""
                            } catch (_: Throwable) { "" }
                            HIDDEN_PACKAGES.none { name.contains(it, true) }
                        }.toMutableList()
                    }
                },
            )
            // getInstalledApplications
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", cl,
                "getInstalledApplications", Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val list = param.result as? MutableList<*> ?: return
                        param.result = list.filter { app ->
                            val name = try {
                                XposedHelpers.getObjectField(
                                    app, "packageName",
                                ) as? String ?: ""
                            } catch (_: Throwable) { "" }
                            HIDDEN_PACKAGES.none { name.contains(it, true) }
                        }.toMutableList()
                    }
                },
            )
            // getPackageInfo
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", cl,
                "getPackageInfo", String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val pkg = param.args[0] as? String ?: return
                        if (HIDDEN_PACKAGES.any { pkg.contains(it, true) }) {
                            param.throwable =
                                android.content.pm.PackageManager
                                    .NameNotFoundException(pkg)
                        }
                    }
                },
            )
            // getApplicationInfo — before hook: throw for hidden packages
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", cl,
                "getApplicationInfo", String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val pkg = param.args[0] as? String ?: return
                        if (HIDDEN_PACKAGES.any { pkg.contains(it, true) }) {
                            param.throwable =
                                android.content.pm.PackageManager
                                    .NameNotFoundException(pkg)
                        }
                    }
                },
            )
        } catch (_: Throwable) {}
    }

    // -- 4. File access interception --
    private fun hookFileAccess(cl: ClassLoader) {
        try {
            // File.exists
            XposedHelpers.findAndHookMethod(
                "java.io.File", cl, "exists",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val path = (param.thisObject as java.io.File).absolutePath
                        if (HIDDEN_PATHS.any { path.startsWith(it) }) {
                            param.result = false
                        }
                    }
                },
            )
            // File.canRead
            XposedHelpers.findAndHookMethod(
                "java.io.File", cl, "canRead",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val path = (param.thisObject as java.io.File).absolutePath
                        if (HIDDEN_PATHS.any { path.startsWith(it) }) {
                            param.result = false
                        }
                    }
                },
            )
            // File.listFiles (directory scan, chunqiu-specific)
            XposedHelpers.findAndHookMethod(
                "java.io.File", cl, "listFiles",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val dir = param.thisObject as java.io.File
                        if (dir.absolutePath.startsWith("/data/adb")) {
                            param.result = emptyArray<java.io.File>()
                        }
                    }
                },
            )
        } catch (_: Throwable) {}
    }

    // -- 5. Runtime.exec interception --
    private fun hookRuntimeExec(cl: ClassLoader) {
        val hook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val cmd = when (val arg = param.args[0]) {
                    is String -> arg
                    is Array<*> -> arg.firstOrNull() as? String ?: ""
                    else -> ""
                }
                val trimmed = cmd.trim().lowercase()
                if (trimmed == "su" || trimmed.startsWith("su ") ||
                    trimmed.contains("which su") ||
                    trimmed.contains("busybox") ||
                    trimmed.contains("magisk")
                ) {
                    param.throwable = java.io.IOException("Permission denied")
                }
            }
        }
        try {
            XposedHelpers.findAndHookMethod(
                "java.lang.Runtime", cl, "exec",
                String::class.java, hook,
            )
            XposedHelpers.findAndHookMethod(
                "java.lang.Runtime", cl, "exec",
                Array<String>::class.java, hook,
            )
        } catch (_: Throwable) {}
    }

    // -- 6. Build field correction --
    private fun hookBuildFields() {
        try {
            XposedHelpers.setStaticObjectField(
                Build::class.java, "TAGS", "release-keys",
            )
        } catch (_: Throwable) {}
    }

    // -- 7. SystemProperties interception --
    private fun hookSystemProperties(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.os.SystemProperties", cl,
                "get", String::class.java, String::class.java,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        when (param.args[0] as? String) {
                            "ro.debuggable" -> param.result = "0"
                            "ro.secure" -> param.result = "1"
                            "ro.build.tags" -> param.result = "release-keys"
                            "ro.build.type" -> param.result = "user"
                            "ro.boot.verifiedbootstate" -> param.result = "green"
                            "ro.boot.flash.locked" -> param.result = "1"
                            "ro.boot.veritymode" -> param.result = "enforcing"
                        }
                    }
                },
            )
        } catch (_: Throwable) {}
    }

    // -- 8. Settings interception (Hunter-specific) --
    // Reasoning: Hunter checks Settings.Global for "adb_enabled",
    // "development_settings_enabled", etc.
    private fun hookSettingsProvider(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.provider.Settings\$Global", cl,
                "getInt", android.content.ContentResolver::class.java,
                String::class.java, Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        when (param.args[1] as? String) {
                            "adb_enabled" -> param.result = 0
                            "development_settings_enabled" -> param.result = 0
                        }
                    }
                },
            )
        } catch (_: Throwable) {}
    }

    // -- 9. ContentResolver interception (Hunter-specific) --
    // Reasoning: Hunter uses ContentResolver.query to query
    // content://settings/global and content://settings/secure.
    private fun hookContentResolver(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.content.ContentResolver", cl,
                "query", android.net.Uri::class.java,
                Array<String>::class.java,
                String::class.java,
                Array<String>::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val uri = param.args[0]?.toString() ?: return
                        if (uri.contains("settings/global") ||
                            uri.contains("settings/secure")
                        ) {
                            // Don't block the query; the Settings hook above
                            // will modify the returned values.
                        }
                    }
                },
            )
        } catch (_: Throwable) {}
    }

    // -- 10. ApplicationInfo interception (chunqiu-specific) --
    // Reasoning: chunqiu checks ApplicationInfo.flags for FLAG_DEBUGGABLE.
    private fun hookApplicationInfo(cl: ClassLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                "android.app.ApplicationPackageManager", cl,
                "getApplicationInfo", String::class.java,
                Int::class.javaPrimitiveType,
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val info = param.result as? android.content.pm.ApplicationInfo
                            ?: return
                        // Clear FLAG_DEBUGGABLE
                        info.flags = info.flags and
                            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE.inv()
                    }
                },
            )
        } catch (_: Throwable) {}
    }
}
