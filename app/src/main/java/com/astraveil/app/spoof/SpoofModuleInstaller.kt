package com.astraveil.app.spoof

import android.content.Context
import com.astraveil.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 通过 root 安装/管理 Zygisk 模块。
 *
 * 推理：Magisk 模块安装本质是将文件解压到
 * /data/adb/modules/<id>/，然后 Magisk 在下次启动时加载。
 * 管理器可以通过 root shell 直接完成这个过程，
 * 无需用户手动在 Magisk 中刷入 zip。
 */
object SpoofModuleInstaller {

    private const val MODULE_ID = "astraveil-spoof"
    private const val MODULE_PATH = "/data/adb/modules/$MODULE_ID"

    enum class ModuleStatus {
        NOT_INSTALLED,
        INSTALLED_DISABLED,
        INSTALLED_ENABLED,
        UNKNOWN,
    }

    suspend fun getStatus(context: Context): ModuleStatus =
        withContext(Dispatchers.IO) {
            runCatching {
                val provider = activeProvider() ?: return@withContext ModuleStatus.UNKNOWN
                val exists = provider.execute(
                    "[ -f $MODULE_PATH/module.prop ] && echo yes || echo no"
                ).stdout.trim()
                if (exists != "yes") return@withContext ModuleStatus.NOT_INSTALLED

                val disabled = provider.execute(
                    "[ -f $MODULE_PATH/disable ] && echo yes || echo no"
                ).stdout.trim()
                if (disabled == "yes") ModuleStatus.INSTALLED_DISABLED
                else ModuleStatus.INSTALLED_ENABLED
            }.getOrDefault(ModuleStatus.UNKNOWN)
        }

    /**
     * 从 assets 安装 Zygisk 模块。
     *
     * 实现条件：
     *   1. AstraVeil-Spoof-v1.0.0.zip 打包在 app/src/main/assets/ 中
     *   2. 通过 root 解压到 /data/adb/modules/
     *   3. 设置正确权限
     *   4. 需要重启生效（Magisk 在 boot 时加载模块）
     */
    suspend fun install(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val provider = activeProvider()
                    ?: error("No root provider available")

                // 1. 从 assets 复制 zip 到临时目录
                val zipPath = "/data/local/tmp/astraveil-spoof.zip"
                context.assets.open("AstraVeil-Spoof-v1.0.0.zip").use { input ->
                    java.io.File(context.cacheDir, "spoof.zip").outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
                provider.execute(
                    "cp ${context.cacheDir}/spoof.zip $zipPath"
                )

                // 2. 解压到模块目录
                provider.execute("rm -rf $MODULE_PATH")
                provider.execute("mkdir -p $MODULE_PATH")
                provider.execute(
                    "cd $MODULE_PATH && unzip -o $zipPath"
                )

                // 3. 权限
                provider.execute("chmod -R 0755 $MODULE_PATH")
                provider.execute("chmod 0644 $MODULE_PATH/module.prop")

                // 4. 清理
                provider.execute("rm -f $zipPath")

                // 5. 创建配置目录
                provider.execute("mkdir -p /data/adb/astraveil/spoof")
                provider.execute("chmod 0755 /data/adb/astraveil")
                provider.execute("chmod 0755 /data/adb/astraveil/spoof")
            }
        }

    suspend fun enable(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val provider = activeProvider() ?: error("No root")
                provider.execute("rm -f $MODULE_PATH/disable")
            }
        }

    suspend fun disable(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val provider = activeProvider() ?: error("No root")
                provider.execute("touch $MODULE_PATH/disable")
            }
        }

    suspend fun uninstall(context: Context): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val provider = activeProvider() ?: error("No root")
                provider.execute("rm -rf $MODULE_PATH")
                provider.execute("rm -rf /data/adb/astraveil/spoof")
            }
        }

    private suspend fun activeProvider() = runCatching {
        ProviderRegistry.activeProvider()
    }.getOrNull()
}
