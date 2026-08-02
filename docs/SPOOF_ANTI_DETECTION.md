# AstraVeil Spoof Anti-Detection

反检测层设计笔记。每条对策均标注其推理路径与不确定性。

## 1. Hook 痕迹隐藏

部分检测代码扫描 `/proc/self/maps` 寻找可疑的 `.so` 加载。
Zygisk 模块的 `.so` 会出现在 maps 中。对策推理：

1. Zygisk API 本身在 `postAppSpecialize` 后会自动卸载模块 `.so`，
   前提是设置了 `DLCLOSE_MODULE_LIBRARY`。
2. 但我们不能卸载（hook 需要留在进程中）。
3. 替代方案：修改 `.so` 的文件名，使其看起来像系统库。

### 实现（在 `customize.sh` 中）

```bash
mv $MODPATH/zygisk/arm64-v8a.so $MODPATH/zygisk/libandroid_runtime_ext.so
```

## 2. Dobby hook 痕迹

Dobby 的 inline hook 会修改函数开头的指令（写入跳转）。
检测代码可以检查 `__system_property_get` 的前几条指令是否是
正常的函数序言（`stp x29, x30, [sp, #-xx]!`）。
如果被 hook，开头会是 `br x16` 或 `ldr x16, [pc, #8]` 等跳转指令。

### 对策（超纲，推理）

1. 使用 GOT hook 替代 inline hook（不修改代码段）
2. 或 hook `__system_property_find` 返回自定义 `prop_info` 结构
   （更复杂但更隐蔽）

当前实现使用 Dobby inline hook（简单可靠），
GOT hook 作为 v2 改进方向。

**不确定标注**：GOT hook 在 Android 14+ 的 linker namespace 隔离下
可能需要额外处理，需实测。

## 3. Root 隐藏集成

检测机型的应用通常同时检测 root。
AstraVeil 作为 root 管理器，应该能联动 Shamiko / DenyList。

### 实现路径

- **Magisk DenyList**: `magisk --denylist add com.target.app`
- **Shamiko**: 自动隐藏（无需额外配置）
- **KernelSU**: `ksu hide add com.target.app`

### Kotlin 集成示例

```kotlin
// 在 DeviceSpoofScreen 的选项中增加：
suspend fun hideRootForApp(context: Context, packageName: String) {
    val provider = activeProvider() ?: return
    when {
        provider.displayName.contains("magisk", true) ->
            provider.execute("magisk --denylist add $packageName")
        provider.displayName.contains("kernelsu", true) ->
            provider.execute("ksu hide add $packageName")
        // APatch: 不确定标注，命令格式需实测
    }
}
```

## 4. 不确定性清单

| 项 | 状态 | 说明 |
|----|------|------|
| Zygisk API `preAppSpecialize` 时机 | 确定 | 在 Zygote fork 后、应用 `main()` 前 |
| Dobby inline hook 在 Android 15 上的兼容性 | 需实测 | Android 15 的 CFI (Control Flow Integrity) 可能影响 |
| `__system_property_read_callback` hook | 确定 | 同步调用，栈上 trampoline 有效 |
| `glGetString` hook 通过 `dlsym` 绕过 | 部分覆盖 | 已 hook `dlsym`，但 `android_dlopen_ext` 路径未覆盖 |
| LSPosed `setStaticObjectField` 对 `Build` 的效果 | 确定 | LSPosed 官方文档和大量模块验证 |
| `ksud setprop` / `apd setprop` 存在性 | 需实测 | 命令可能不存在，需回退到 `setprop` |
| GOT hook 替代 inline hook | 推理 | Android 14+ linker namespace 隔离可能需额外处理 |
| `sepolicy.rule` 的 `zygisk_exec` 上下文 | 需实测 | 不同 Magisk 版本的 SELinux 上下文名可能不同 |
| 信号通知方案 (SIGUSR1) | 推理失败 | Java 层 static final 无法运行时修改，只对 Native 有效 |

## 5. 免重启动态生效（推理，未实现）

配置变更后已运行的应用如何不重启生效？

### 方案 A（当前实现）：`am force-stop <package>`

- 优点：简单可靠
- 缺点：应用状态丢失（未保存的数据）

### 方案 B：信号通知

管理器 → `kill -SIGUSR1 <pid>`，
Zygisk 模块注册 SIGUSR1 handler → 重新读取配置 → 更新 `g_spoofed_props`。

- 优点：应用不重启，状态保留
- 缺点：
  1. `Build.MODEL` 等 static final 字段已在类加载时缓存，
     信号处理后只能影响后续的 `__system_property_get` 调用，
     无法改变已缓存的 Build 字段
  2. 需要知道目标应用的 PID（通过 `pidof` 获取）
- 结论：只对 Native 层 hook 有效，Java 层仍需重启进程

### 方案 C：Zygisk 模块 inotify 监听

模块在 `preAppSpecialize` 时启动一个线程，
`inotify_watch(/data/adb/astraveil/spoof/<package>.json)`，
文件变更 → 重新读取 → 更新 hook 表。

- 优点：完全自动，无需 force-stop
- 缺点：额外线程开销；同样无法改变已缓存的 Build 字段
- 实现条件：需要在 Zygisk 模块中创建 pthread + inotify fd

### 结论

Java 层的 `Build.MODEL` 等静态字段**无法在进程运行期间修改**
（`static final` 在类加载时初始化，Xposed 的 `setStaticObjectField`
只在类加载时有效）。因此"免重启"只对 Native 层属性查询有效。
对于需要完整伪装的场景，`am force-stop` 是最可靠的方案。
