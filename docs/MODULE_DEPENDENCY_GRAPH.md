# AstraVeil 模块依赖图

## Gradle 模块依赖关系

```
:app
├── :core
├── :providers
│   └── :core
├── :modules
│   ├── :core
│   ├── :providers
│   ├── :native
│   │   └── :core
│   └── :sdk
│       ├── :core
│       └── :providers
└── (standalone libs: Compose, Navigation, Haze, kotlinx)
```

## 模块职责

| 模块 | 职责 | 对外暴露 |
|------|------|----------|
| `:core` | AstraCore 引擎：CapabilityEngine, PermissionEngine, EventBus, ConfigManager, AstraLogger, SecurityManager, IPC协议, 能力租约, 预测引擎, 审计链, 快照 | 全部 public |
| `:providers` | RootProvider 抽象层 + Magisk/KernelSU/APatch/AstraRoot 实现 + ProviderWatchdog | RootProvider 接口, ProviderRegistry |
| `:modules` | .avm 模块运行时：ModuleManager, ModuleRuntime, TrustGate, NativeModuleLoadPolicy, 依赖图, 兼容性检查 | ModuleManager, AstraModule |
| `:native` | C++20 JNI 桥接 (libastra_native.so)：procfs/sysfs 探测, su 扫描, Rust FFI 链接 | NativeBridge (Kotlin) |
| `:sdk` | 公共 SDK 门面：第三方模块开发者使用的稳定 API | AstraSdkFacade |
| `:app` | AstraUI Compose 界面 + ViewModel + 终端 + 超级用户 + 设置 + 设备伪装 | MainActivity |

## Rust 与 Daemon

```
:rust (Cargo, 非 Gradle 模块)
├── policy.rs          → 策略引擎 (Allow/Deny/RequireApproval)
├── capability_token.rs → HMAC-SHA256 能力令牌
├── zk_capability.rs   → 零知识证明
├── ffi.rs             → C FFI 导出 (policy_check, policy_is_available)
└── 通过 cargo-ndk 交叉编译 → libastra_rust.a → 被 :native CMake 链接

:daemon (CMake, 非 Gradle 模块)
├── astrad 可执行文件 → Magisk service.sh 启动 → Unix Domain Socket IPC
├── PolicyBridge → 调用 Rust policy_check (weak fallback = DENY, fail-closed)
├── probe_detector → 真实能力探测 (getuid/unshare//proc//sys)
├── provider_detector → 真实后端检测 (文件检查 + 功能性验证)
├── lease_tracker → 租约执行点 (PolicyBridge 查询)
├── capability_tree → seL4 启发的能力委托树
├── module_bus → 模块间通信总线
└── json_codec → nlohmann/json 结构化响应
```

## 依赖方向规则

1. `:core` 不依赖任何其他 AstraVeil 模块（最底层）
2. `:providers` 只依赖 `:core`
3. `:modules` 依赖 `:core` + `:providers` + `:native` + `:sdk`
4. `:native` 只依赖 `:core`（C++ 层通过 JNI 回调 Kotlin）
5. `:sdk` 依赖 `:core` + `:providers`（`api` 而非 `implementation`，对模块开发者可见）
6. `:app` 依赖所有模块（最顶层）
7. `:rust` 和 `:daemon` 不在 Gradle 模块图中，通过 cargo/cmake 独立构建

## 循环依赖检查

当前无循环依赖。`ProviderWatchdog` (:providers) 引用 `ProviderRegistry` (同模块)，`ExecutionRouter` (:app) 引用 `AstraVeilApplication.daemonManager` (:app) — 均为同模块内部引用，不构成跨模块循环。
