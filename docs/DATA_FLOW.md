# AstraVeil — 数据流文档

## 1. 策略评估数据流

### 同步路径（App 内）

```
用户操作 → :app (Compose) → :sdk → :core (PolicyEngine.evaluate)
  → :modules (路由) → :native (JNI) → Rust Engine (astra_policy_evaluate)
  → 决策返回 → UI 更新
```

延迟预算: < 5ms (P99)

### 异步路径（Daemon 审计）

```
:modules → IpcEnvelope(POLICY_QUERY) → Unix Domain Socket
  → daemon/astrad (审计 + SELinux) → IpcEnvelope(POLICY_RESPONSE)
  → :modules → AstraEventBus → :app (可选 UI 更新)
```

延迟预算: < 50ms (P99, 异步不阻塞主路径)

## 2. 模块生命周期数据流

```
AstraHub (下载 .avm) → 验证签名 → :modules (ModuleManager.install)
  → TrustGate (strict=true) → 解包 (Zip Slip 防护) → 注册
  → :native (System.load, NativeModuleLoadPolicy 门控)
  → daemon (MODULE_COMMAND:LOAD, 沙箱创建)
  → MODULE_EVENT:ACTIVE → :core EventBus → :app UI 更新
```

## 3. IPC 连接管理

### 连接建立
```
:app (AstraDaemonClient) → socket(AF_UNIX) → connect("/dev/astra/astrad.sock")
  → daemon accept() → SO_PEERCRED UID 白名单验证
  → 协商 protocol_version → 连接就绪
```

### 心跳机制
- 每 30 秒: Client → Heartbeat, Server → HeartbeatAck
- 90 秒无响应 → 断开 → 重连 (指数退避 1s→60s)

### 错误处理

| 错误 | 处理 |
|------|------|
| Socket 断开 | 自动重连，指数退避 |
| 消息超时 (15s) | 返回 null，标记 OFFLINE |
| 消息过大 (>8MB) | 拒绝，记录错误 |
| 反序列化失败 | 返回 null，连接保持 |

## 4. 终端执行数据流

### 交互式终端（P1-12 独立通道）
```
用户输入命令 → TerminalViewModel → TrustedInteractiveSession (审批+审计)
  → ShellSession (持久 su shell) → 命令执行 → 流式输出
  → CommandAuditLogger (JSONL 审计日志)
```

### 模块执行（P1-11 统一通道）
```
模块请求 → ExecutionRouter.executeForModule()
  → daemon IPC (结构化 ExecuteRequest)
  → PolicyBridge.checkWith (Rust, fail-closed)
  → ALLOW → executor.execute(command)
  → DaemonResponse → App 解析
```

## 5. 数据格式对照

| 路径 | 格式 | 原因 |
|------|------|------|
| App ↔ SDK | Kotlin 对象 | 类型安全 |
| SDK ↔ Core | Kotlin 对象 | 同进程 |
| Core ↔ Native | JNI (原始类型) | JNI 限制 |
| Native ↔ Rust | C ABI (指针 + 长度) | FFI |
| App ↔ Daemon | JSON (nlohmann) | 跨进程、跨语言 |
| AstraHub ↔ App | HTTPS + JSON | 网络传输 |
| 策略存储 | JSON (ConfigManager) | 简单、可读 |
| 审计日志 | JSONL (append-only) | 防篡改、可导出 |
