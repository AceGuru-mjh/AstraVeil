# AstraVeil Spoof Config Protocol

Zygisk 模块、LSPosed 模块、管理器三方共享的 JSON 格式规范。

配置文件由 AstraVeil 管理器（root）写入，由 Zygisk 模块（preAppSpecialize）
和 LSPosed 模块（handleLoadPackage）读取。三方必须遵循同一份 schema，
否则会出现层间不一致（被检测的常见根因）。

## 1. 配置目录

```
/data/adb/astraveil/spoof/
├── global.json                  ← 全局兜底配置
├── <package-name>.json          ← per-app 覆盖配置（优先于 global）
└── ...
```

- 权限：目录 `0755`、文件 `0644`。Zygisk 模块以目标应用 UID 运行，必须可读。
- 读取顺序：先 per-app，未命中再 global，都未命中则返回 disabled 配置。

## 2. 全局配置示例

`/data/adb/astraveil/spoof/global.json`：

```json
{
  "enabled": true,
  "profile": "Pixel 9 Pro",
  "resetprop_on_boot": false,
  "android_id": "a3f5c8d90e1b2f34",
  "sim_operator": "T-Mobile",
  "screen_width": 1344,
  "screen_height": 2992,
  "density": 3.0,
  "props": {
    "ro.product.model": "Pixel 9 Pro",
    "ro.product.brand": "google",
    "ro.product.manufacturer": "Google",
    "ro.product.device": "caiman",
    "ro.product.name": "caiman",
    "ro.product.odm.model": "Pixel 9 Pro",
    "ro.product.vendor.model": "Pixel 9 Pro",
    "ro.product.system.model": "Pixel 9 Pro",
    "ro.product.product.model": "Pixel 9 Pro",
    "ro.product.system_ext.model": "Pixel 9 Pro",
    "ro.product.first_api_level": "34",
    "ro.build.fingerprint": "google/caiman/caiman:15/AP3A.250605.015/13187197:user/release-keys",
    "ro.build.description": "caiman-user 15 AP3A.250605.015 13187197 release-keys",
    "ro.build.id": "AP3A.250605.015",
    "ro.build.version.incremental": "13187197",
    "ro.build.display.id": "AP3A.250605.015",
    "ro.build.version.security_patch": "2025-06-05",
    "ro.build.flavor": "caiman-user",
    "ro.build.characteristics": "default",
    "ro.build.tags": "release-keys",
    "ro.build.type": "user",
    "ro.board.platform": "zuma",
    "ro.product.board": "zuma",
    "ro.soc.model": "Tensor G4",
    "ro.soc.manufacturer": "Google"
  },
  "gl": {
    "renderer": "Immortalis-G715",
    "vendor": "ARM"
  }
}
```

## 3. Per-app 覆盖示例

`/data/adb/astraveil/spoof/com.target.app.json`：

```json
{
  "enabled": true,
  "profile": "Galaxy S25 Ultra",
  "props": { "..." : "同上结构，但值不同" },
  "gl": {
    "renderer": "Adreno (TM) 830",
    "vendor": "Qualcomm"
  }
}
```

## 4. 字段语义

| 字段                  | 类型      | 消费者               | 说明 |
|----------------------|-----------|----------------------|------|
| `enabled`            | bool      | Zygisk + LSPosed     | 总开关。`false` 时跳过所有 hook |
| `profile`            | string    | 仅日志/UI            | 人类可读的档案名称 |
| `resetprop_on_boot`  | bool      | Magisk service.sh    | `true` → 启动时全局 resetprop 兜底 |
| `android_id`         | string    | LSPosed SettingsHook | 16 位十六进制；空则不拦截 |
| `sim_operator`       | string    | LSPosed TelephonyHook| `getSimOperatorName` / `getNetworkOperatorName` 返回值 |
| `screen_width/height`| int       | LSPosed DisplayHook  | 0 → 不修改 DisplayMetrics |
| `density`            | float     | LSPosed DisplayHook  | 0 → 不修改 density |
| `props`              | object    | Zygisk property_hook | key/value 形式的 `ro.*` 属性 |
| `gl.renderer`        | string    | Zygisk gl_spoof      | `glGetString(GL_RENDERER)` 返回值 |
| `gl.vendor`          | string    | Zygisk gl_spoof      | `glGetString(GL_VENDOR)` 返回值 |

## 5. 写入约定

- 写入由 `SpoofConfigManager` 通过 root provider 完成（管理器本身无 `/data/adb` 写权限）。
- 单条 `echo '<json>' > file` 写入，避免 partial write 竞态。
- JSON 字符串中的单引号需 `'\\''` 转义（shell 单引号字符串内的标准 escape）。
- 写入后立即 `chmod 0644`，确保 Zygisk 模块（应用 UID）可读。

## 6. 一致性维护

三层组合拳引入了**层间不一致风险**：例如用户手动 `resetprop ro.product.model "Pixel 8"`
而 JSON 配置仍是 Pixel 9 Pro → 应用 Java 层读 `Build.MODEL` 得到 Pixel 9 Pro，
Native 层 `getprop` 得到 Pixel 8 → 矛盾 → 被检测。

`SpoofConsistencyHealer`（推理设计，未实现）会周期性比对 JSON 配置与 `getprop` 实际值，
发现矛盾时以 JSON 为准重新 `resetprop`。
