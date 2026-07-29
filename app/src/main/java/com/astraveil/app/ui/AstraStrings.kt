package com.astraveil.app.ui

import java.util.Locale

/**
 * Centralised locale-aware string table for the AstraUI Compose layer.
 *
 * Follows the system locale at first access: Chinese (zh) → Chinese
 * strings, everything else → English. A configuration change (e.g.
 * the user switches language in system settings) is picked up on the
 * next process restart, which is the standard Android behaviour for
 * a non-resource-based string table.
 *
 * Usage:
 * ```
 * Text(text = AstraStrings.modulesTitle)          // composable
 * label = AstraStrings.navDashboard               // data class
 * AstraStrings.installFailed(t.message)           // viewmodel
 * ```
 *
 * Why a Kotlin object rather than Android `strings.xml`?
 *  - A single file is easier to review for translation correctness.
 *  - No risk of `R.string.xxx` ID mismatches across 11 screen files
 *    (this project cannot be compiled in the dev sandbox, so a
 *    resource-ID typo would not be caught until CI).
 *  - Works in non-`@Composable` contexts (ViewModel, data classes).
 *
 * TODO: migrate to standard res/values-zh/strings.xml + stringResource()
 *   before v0.2.0. The current object is a pragmatic compromise for the
 *   no-compile sandbox; the standard resource approach supports plurals,
 *   formatted strings, RTL, and triggers Compose recomposition on
 *   locale change (this object does not — it reads Locale.getDefault()
 *   once per property access but is not a State).
 *
 * Brand names ("AstraVeil", "AstraRoot", "Magisk", "KernelSU",
 * "APatch", "AstraVM", "AstraDaemon") and file/format names
 * (".avm", "module.json", "SHA-256") are intentionally NOT
 * translated.
 */
object AstraStrings {

    private val isZh: Boolean
        get() = Locale.getDefault().language == "zh"

    /** Pick the English or Chinese variant. */
    private fun s(en: String, zh: String): String = if (isZh) zh else en

    // ---- Helper for dynamic strings ----
    fun activeCount(n: Int): String =
        if (isZh) "$n 个活跃" else "$n Active"

    fun authorizedApps(authorized: Int, total: Int): String =
        if (isZh) "已授权应用：$authorized / $total 个活跃配置"
        else "Authorized Apps: $authorized of $total active profiles"

    fun versionLabel(version: String): String =
        if (isZh) "版本 $version" else "Version $version"

    fun permissionsCount(n: Int): String =
        if (isZh) "权限 ($n)" else "Permissions ($n)"

    fun installSuccess(name: String): String =
        if (isZh) "'$name' 已安装" else "'$name' installed."

    fun installFailed(reason: String?): String =
        if (isZh) "安装失败：${reason ?: "未知错误"}"
        else "Install failed: ${reason ?: "unknown error"}"

    fun startFailed(reason: String?): String =
        if (isZh) "启动失败：${reason ?: "未知错误"}"
        else "Start failed: ${reason ?: "unknown error"}"

    fun stopFailed(reason: String?): String =
        if (isZh) "停止失败：${reason ?: "未知错误"}"
        else "Stop failed: ${reason ?: "unknown error"}"

    fun uninstallFailed(reason: String?): String =
        if (isZh) "卸载失败：${reason ?: "未知错误"}"
        else "Uninstall failed: ${reason ?: "unknown error"}"

    fun loadModulesFailed(reason: String?): String =
        if (isZh) "加载模块失败：${reason ?: "未知错误"}"
        else "Failed to load modules: ${reason ?: "unknown error"}"

    fun cannotScanModule(reason: String): String =
        if (isZh) "无法扫描模块：$reason" else "Cannot scan module: $reason"

    fun manifestCouldNotBeRead(status: String): String =
        if (isZh) "无法读取清单（$status）。" else "Manifest could not be read ($status)."

    // ---- Navigation ----
    val navDashboard get() = s("Dashboard", "仪表盘")
    val navCapability get() = s("Capability", "能力")
    val navProvider get() = s("Provider", "提供者")
    val navModules get() = s("Modules", "模块")
    val navSuperuser get() = s("Superuser", "超级用户")
    val navAbout get() = s("About", "关于")
    val navSettings get() = s("Settings", "设置")

    // ---- Dashboard ----
    val dashSystemStatus get() = s("System Status", "系统状态")
    val dashDeviceIntelligence get() = s("Device Intelligence", "设备信息")
    val dashCompatibilityAssessment get() = s("Compatibility Assessment", "兼容性评估")
    val dashPrivilegeBackend get() = s("Privilege Backend", "特权后端")
    val dashCapabilities get() = s("Capabilities", "能力")
    val dashModulesTitle get() = s("Modules", "模块")
    val dashSecurity get() = s("Security", "安全")
    val dashEnvLevel get() = s("Environment Level: ", "环境等级：")
    val dashSystemWarnings get() = s("System Warnings / Recommendations:", "系统警告 / 建议：")
    val dashAstraModules get() = s("Astra Modules (.avm)", "Astra 模块 (.avm)")
    val dashQuickActions get() = s("QUICK ACTIONS", "快捷操作")
    val dashTestRootCap get() = s("Test Root Capability", "测试 Root 能力")
    val dashInstallModule get() = s("Install Module", "安装模块")
    val dashRootTest get() = s("Root Test", "Root 测试")

    // ---- Capability ----
    val capTitle get() = s("Capability", "能力")
    val capSubtitle get() = s("Detailed device + runtime capability probe results.",
        "设备与运行时能力探测详细结果。")
    val capMode get() = s("Mode", "模式")
    val capDetected get() = s("Detected", "已检测")

    // ---- Provider ----
    val provTitle get() = s("Provider", "提供者")
    val provSubtitle get() = s(
        "AstraVeil abstracts over Magisk, KernelSU, APatch and its own AstraRoot backend.",
        "AstraVeil 抽象了 Magisk、KernelSU、APatch 及自研的 AstraRoot 后端。")
    val provNoBackend get() = s("No root backend detected", "未检测到 Root 后端")
    val provNoBackendHint get() = s(
        "This is expected during Phase 0. AstraVeil runs in capability-probe mode without an active provider.",
        "这在 Phase 0 阶段是正常的。AstraVeil 在无活跃提供者时以能力探测模式运行。")
    val provTesting get() = s("Testing…", "测试中…")
    val provTestRoot get() = s("  Test Root", "  测试 Root")

    // ---- Superuser / Root Manager ----
    val suTitle get() = s("Superuser", "超级用户")
    val suSubtitle get() = s(
        "AstraVeil acts as an identity-agnostic brokered control plane, managing root capabilities for third-party apps and modules.",
        "AstraVeil 作为身份无关的代理控制平面，为第三方应用和模块管理 Root 能力。")
    val suSecureBroker get() = s("Astra Secure Broker", "Astra 安全代理")

    // ---- About ----
    val aboutTagline get() = s("Android Root Capability Operating Layer",
        "Android Root 能力操作系统层")
    val aboutTechStack get() = s("TECHNOLOGY STACK", "技术栈")
    val aboutRoleAstraUI get() = s("AstraUI", "AstraUI")
    val aboutRoleSecurityEngine get() = s("Security Policy Engine", "安全策略引擎")
    val aboutRoleDaemon get() = s("AstraDaemon + Native Bridge", "AstraDaemon + 原生桥接")
    val aboutRoleIpc get() = s("IPC Protocol", "IPC 协议")
    val aboutRoleSandbox get() = s("Sandbox", "沙箱")

    // ---- Diagnostics ----
    val diagTitle get() = s("Diagnostics", "诊断")
    val diagSubtitle get() = s(
        "Analyze device capability integrity, trace warnings, and export professional reports for bug tracking.",
        "分析设备能力完整性、追踪警告，并导出专业报告用于问题跟踪。")
    val diagHealthScanner get() = s("System Health Scanner", "系统健康扫描器")
    val diagScanDescription get() = s(
        "Generate a diagnostic report capturing kernel, SELinux, and provider states. The report is exported locally as 'diagnostics.astra-report'.",
        "生成诊断报告，捕获内核、SELinux 和提供者状态。报告将导出为本地文件 'diagnostics.astra-report'。")
    val diagScanning get() = s("  Scanning…", "  扫描中…")
    val diagRunScan get() = s("  Run Full Scan", "  运行完整扫描")

    // ---- Settings ----
    val settingsTitle get() = s("Settings", "设置")
    val settingsSubtitle get() = s("Astra Control Center", "Astra 控制中心")

    // ---- Modules screen ----
    val modTitle get() = s("Modules", "模块")
    val modSubtitle get() = s(
        "Astra Modules extend AstraVeil with isolated, permissioned packages.",
        "Astra 模块以隔离的、受权限控制的包扩展 AstraVeil。")
    val modInstalled get() = s("Installed Modules", "已安装模块")
    val modEmptyTitle get() = s("No Astra Modules installed yet", "尚未安装 Astra 模块")
    val modEmptyHint get() = s(
        "Tap + or \"Install a .avm file\" to get started.",
        "点击 + 或\"安装 .avm 文件\"开始。")
    val modInstallCtaTitle get() = s("Install an Astra Module", "安装 Astra 模块")
    val modInstallCtaDesc get() = s(
        "Pick a .avm file. AstraVeil will pre-parse its manifest and show you the real module name, version, and requested permissions before installing.",
        "选择一个 .avm 文件。AstraVeil 将在安装前预解析其清单，并向您展示真实的模块名称、版本和请求的权限。")
    val modInstallFileBtn get() = s("Install a .avm file", "安装 .avm 文件")
    val modFormatTitle get() = s("The .avm format", ".avm 格式")
    val modFormatDesc get() = s(
        "An Astra Module is a signed .avm bundle that contains everything AstraVeil needs to safely install and run an extension:",
        "Astra 模块是一个签名的 .avm 包，包含 AstraVeil 安全安装和运行扩展所需的一切：")
    val modFormatManifestDesc get() = s(
        "Manifest: id, version, entrypoint, dependencies.",
        "清单：id、版本、入口点、依赖。")
    val modFormatRuntimeDesc get() = s(
        "Kotlin/Native or DEX code, executed inside the AstraVM sandbox.",
        "Kotlin/Native 或 DEX 代码，在 AstraVM 沙箱中执行。")
    val modFormatAssetsDesc get() = s(
        "Static resources shipped with the module.",
        "模块附带的静态资源。")
    val modFormatPermissionDesc get() = s(
        "Capability grants requested by the module (Mount, Hook, Namespace, …).",
        "模块请求的能力授权（挂载、Hook、命名空间等）。")
    val modScanningPackage get() = s("Scanning package…", "正在扫描包…")
    val modWorking get() = s("Working…", "处理中…")
    val modDismiss get() = s("Dismiss", "关闭")
    val modStarted get() = s("Started.", "已启动。")
    val modStopped get() = s("Stopped.", "已停止。")
    val modUninstalled get() = s("Module uninstalled.", "模块已卸载。")
    val modFailedReinstall get() = s("Module failed — reinstall required",
        "模块故障 — 需要重新安装")

    // Module state pills
    val stateRunning get() = s("Running", "运行中")
    val stateInstalled get() = s("Installed", "已安装")
    val stateStopped get() = s("Stopped", "已停止")
    val stateFailed get() = s("Failed", "故障")

    // Module action buttons
    val actionStart get() = s("Start", "启动")
    val actionStop get() = s("Stop", "停止")
    val actionUninstall get() = s("Uninstall", "卸载")

    // ---- Security Review Dialog ----
    val secDialogTitle get() = s("Astra Security Scan", "Astra 安全扫描")
    val secFingerprint get() = s("Package fingerprint (SHA-256)", "包指纹 (SHA-256)")
    val secNoPermissions get() = s("No permissions requested.", "未请求任何权限。")
    val secOverallRisk get() = s("Overall risk:", "总体风险：")
    val secRiskSourceLabel get() = s("Risk source", "风险来源")
    val secManifestLabel get() = s("Manifest", "清单")
    val secSignatureLabel get() = s("Signature", "签名")
    val secCancel get() = s("Cancel", "取消")
    val secInstall get() = s("Install", "安装")
    val secInstalling get() = s("Installing…", "安装中…")

    // Risk levels
    val riskLow get() = s("Low", "低")
    val riskMedium get() = s("Medium", "中")
    val riskHigh get() = s("High", "高")
    val riskUnknown get() = s("Unknown", "未知")
    val riskLevelLow get() = s("LOW", "低")
    val riskLevelMedium get() = s("MEDIUM", "中")
    val riskLevelHigh get() = s("HIGH", "高")
    val riskLevelCritical get() = s("CRITICAL", "严重")
    val riskLevelUnknown get() = s("UNKNOWN", "未知")

    // Risk source values
    val srcManifestDeclared get() = s("manifest-declared", "清单声明")
    val srcUndeclared get() = s("undeclared (Phase-0)", "未声明 (Phase-0)")
    val srcNone get() = s("none", "无")

    // Manifest status values
    val manifestOk get() = s("OK", "正常")
    val manifestMissing get() = s("missing", "缺失")
    val manifestMalformed get() = s("malformed", "格式错误")

    // Signature status values
    val sigUnknown get() = s("Unknown", "未知")
    val sigUnsigned get() = s("Unsigned", "未签名")
    val sigUnverified get() = s("Unverified", "未验证")
    val sigVerified get() = s("Verified", "已验证")
    val sigRejected get() = s("Rejected", "已拒绝")

    // Dash em for blank fields
    val dash get() = s("—", "—")
}
