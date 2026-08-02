# AstraVeil Native — Rust 构建集成

## 架构概览

```
┌─────────────────────────────────────────────────────────────────┐
│                      Gradle Build                               │
│                                                                 │
│  :native (build.gradle.kts)                                     │
│  ├── cargoBuild (Exec task)                                     │
│  │   └── cargo ndk -t arm64-v8a -t x86_6a build --release      │
│  │                                                              │
│  ├── checkRustLibs                                              │
│  │   └── 验证 .a 文件存在性                                      │
│  │                                                              │
│  └── externalNativeBuild (CMake)                                │
│      ├── ASTRA_HAVE_RUST=1 → 链接 libastra_rust.a               │
│      └── ASTRA_HAVE_RUST=0 → Kotlin-only path (weak symbol)     │
└─────────────────────────────────────────────────────────────────┘
```

## Rust 集成方式

AstraVeil 使用 **weak symbol** 模式集成 Rust：

- Rust crate (`rust/`) 导出 C FFI 函数：`policy_check`、`policy_is_available`
- 当 `libastra_rust.a` 被链接时（`ASTRA_HAVE_RUST=1`），符号解析到 Rust 实现
- 当未链接时（`ASTRA_HAVE_RUST=0`），daemon 的 `PolicyBridge` 使用 weak fallback → **DENY（fail-closed）**
- `:native` 的 C++ 层（`astra_native.cpp`）不直接调用 Rust FFI；Rust 策略引擎由 daemon 的 `PolicyBridge` 调用

## 快速开始

### 方式 1：Gradle 自动构建（推荐）

```bash
# cargoBuild 会在 CMake 构建前自动执行
./gradlew :native:assembleDebug
```

### 方式 2：手动构建 Rust

```bash
cd rust
chmod +x build-android.sh
./build-android.sh
```

### 方式 3：仅构建特定 ABI

```bash
cd rust && ./build-android.sh --abi arm64-v8a
```

## 前置条件

| 工具 | 安装命令 | 验证 |
|------|----------|------|
| Rust | `curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs \| sh` | `rustc --version` |
| cargo-ndk | `cargo install cargo-ndk` | `cargo ndk --version` |
| Android NDK | SDK Manager 或 `sdkmanager --install "ndk;27.2.12479018"` | `$ANDROID_NDK_HOME/ndk-build --version` |
| Rust targets | `rustup target add aarch64-linux-android x86_64-linux-android` | `rustup target list --installed` |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `ASTRA_RUST_ROOT` | Rust 交叉编译输出根目录 | `<project>/rust/target` |

也可在 `gradle.properties` 中设置：

```properties
astra.rust.root=/path/to/rust/target
```

## CMake 发现逻辑

`native/CMakeLists.txt` 中的 Rust 库发现：

```
1. 检查 ASTRA_RUST_LIB 缓存变量（直接路径）
2. 若未设置，从 ASTRA_RUST_ROOT + ANDROID_ABI 推导：
   arm64-v8a   → aarch64-linux-android/release/libastra_rust.a
   x86_64      → x86_64-linux-android/release/libastra_rust.a
   armeabi-v7a → armv7-linux-androideabi/release/libastra_rust.a
   x86         → i686-linux-android/release/libastra_rust.a
3. 文件存在 → ASTRA_HAVE_RUST=1，链接
4. 文件不存在 → ASTRA_HAVE_RUST=0，Kotlin-only path
```

## 降级行为

当 Rust 工具链不可用或构建失败时：

1. `cargoBuild` task 打印警告但**不中断**构建（`isIgnoreExitValue = true`）
2. CMake 检测不到 `.a` 文件 → 设置 `ASTRA_HAVE_RUST=0`
3. Daemon `PolicyBridge` 的 weak symbol fallback 生效
4. **所有策略检查返回 DENY（fail-closed）**
5. 应用功能正常但策略引擎为最严格模式

> ⚠️ 注意：与 stub 返回 ALLOW 不同，AstraVeil 的 fail-closed 设计意味着
> 缺少 Rust 引擎时所有特权操作被拒绝。这是安全设计决策。

## Gradle Tasks

| Task | 说明 |
|------|------|
| `:native:cargoBuild` | 执行 `cargo ndk build --release`（多 ABI） |
| `:native:cargoClean` | 执行 `cargo clean` |
| `:native:checkRustLibs` | 检查 `.a` 文件是否存在 |

## 验证

```bash
# 检查 Rust 库状态
./gradlew :native:checkRustLibs

# 完整构建 + 测试
./gradlew :native:assembleDebug :native:testDebugUnitTest

# 清理所有（含 Rust）
./gradlew :native:clean
```

## 故障排除

| 症状 | 原因 | 解决 |
|------|------|------|
| `cargo: command not found` | Rust 未安装或不在 PATH | `source ~/.cargo/env` |
| `cargo-ndk: command not found` | 未安装 cargo-ndk | `cargo install cargo-ndk` |
| `linker not found` | NDK 未配置 | 设置 `ANDROID_NDK_HOME` |
| CMake 报 `ASTRA_HAVE_RUST=0` | `.a` 文件不存在 | 先运行 `cargoBuild` |
| `undefined reference to policy_check` | Rust 库 ABI 不匹配 | 确认 target triple 正确 |
| 构建成功但策略全部 DENY | Rust 未链接，fail-closed 生效 | 运行 `cargoBuild` 后重新构建 |
