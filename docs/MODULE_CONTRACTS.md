# AstraVeil — 模块间契约规范

> 本文档定义各模块的公开 API 边界、禁止事项、以及依赖方向约束。

## 契约总则

1. **上层依赖下层**：`app → modules → sdk → providers → core → proto`
2. **同层不互相依赖**：`:providers` 不依赖 `:sdk`，反之亦然
3. **`:native` 仅被 `:modules` 依赖**：JNI 复杂度不扩散
4. **`:proto` 被所有层依赖**：协议是公共语言
5. **接口在 `:core` 定义，实现在上层**：依赖倒置

## :core 契约

### 必须提供
- `PolicyEngine` — 策略评估抽象
- `AstraEventBus` — 事件发布/订阅
- `ModuleRegistry` — 模块注册/发现
- `AstraConfig` — 全局配置读取
- `SecurityContext` — 当前安全上下文

### 禁止
- ❌ 不引用 `android.app.*`（Activity, Service, BroadcastReceiver）
- ❌ 不引用 `androidx.compose.*`
- ❌ 不直接调用 JNI / native 方法
- ❌ 不持有 Context（通过接口注入）

## :providers 契约

### 必须提供
- `RootProvider` 接口 + Magisk/KernelSU/APatch/AstraRoot 实现
- `ProviderRegistry` — 后端发现与切换
- `ProviderWatchdog` — 热切换监控

### 禁止
- ❌ 不包含 UI 代码
- ❌ 不直接访问数据库 / 文件（通过 :core 接口）

## :sdk 契约

### 必须提供
- `AstraSdkFacade` — 第三方模块开发者使用的稳定 API
- 通过 `api()` 暴露 `:core` + `:providers`

### 禁止
- ❌ 不暴露内部实现类
- ❌ 不要求调用者持有特殊权限

## :modules 契约

### 必须提供
- `ModuleManager` — .avm 安装/卸载
- `ModuleRuntime` — 模块加载/卸载
- `TrustGate` — 安装时信任验证
- `NativeModuleLoadPolicy` — 原生代码加载门控

### 允许
- ✅ 调用 `:native` JNI
- ✅ 通过 IPC 与 daemon 通信
- ✅ 读写模块私有目录

### 禁止
- ❌ 不直接操作其他模块的内部状态
- ❌ 不绕过 `:core` PolicyEngine 直接做决策

## :native 契约

### 必须提供
- `NativeBridge` — JNI 桥接（nativeInvokeModuleEntry 等）
- Rust FFI 链接（ASTRA_HAVE_RUST=1）或 stub（=0）

### 降级保证
- 当 `ASTRA_HAVE_RUST=0` 时，所有方法必须正常返回（不崩溃）
- `policy_check` 返回 `1` (DENY, fail-closed)
- `policy_is_available` 返回 `0` (not linked)

### 禁止
- ❌ 不在 JNI 层做业务逻辑
- ❌ 不抛出 Java Exception（返回错误码）

## :proto 契约

### 必须遵守
- 字段只增不删（删除用 `reserved`）
- 枚举值 0 为 UNSPECIFIED
- 包名含版本号：`astra.<domain>.v<N>`
- 使用 lite 运行时

### 禁止
- ❌ 不包含业务逻辑
- ❌ 不使用 `google.protobuf.Any`（lite 不支持）

## 依赖违规检测

```bash
# 检测 :core 是否引用了 android.app
grep -rn "import android.app" core/src/ && echo "VIOLATION" && exit 1

# 检测 :native 是否被 :core/:providers/:sdk 直接依赖
grep -rn 'project(":native")' core/ providers/ sdk/ && echo "VIOLATION" && exit 1

# 检测 :proto 是否引用了其他项目模块
grep -rn 'project(' proto/build.gradle.kts && echo "VIOLATION" && exit 1
```
