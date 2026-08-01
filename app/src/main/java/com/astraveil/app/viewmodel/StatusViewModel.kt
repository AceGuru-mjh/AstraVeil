package com.astraveil.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.astraveil.app.BuildConfig
import com.astraveil.core.capability.CapabilityInfo
import com.astraveil.core.runtime.CapabilityStatus
import com.astraveil.core.runtime.ProviderStatus
import com.astraveil.providers.runtime.RuntimeRepository
import com.astraveil.core.runtime.RuntimeStatus
import com.astraveil.providers.RootInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class StatusViewModel(app: Application) : AndroidViewModel(app) {

    private val runtimeRepo = RuntimeRepository()

    enum class DaemonStatus { OFFLINE, CONNECTING, ONLINE }

    data class CapabilityTestResult(
        val name: String,
        val command: String,
        val success: Boolean,
        val output: String,
    )

    data class RootTestResult(
        val providerName: String,
        val overallSuccess: Boolean,
        val tests: List<CapabilityTestResult>,
    )

    data class AuthorizedApp(
        val packageName: String,
        val displayName: String,
        val description: String,
        val rootAuthorized: Boolean,
        val isSystem: Boolean = false
    )

    data class UiState(
        val coreVersion: String = BuildConfig.ASTRAVEIL_VERSION,
        val daemonStatus: DaemonStatus = DaemonStatus.OFFLINE,
        val providerName: String = "None",
        val providerVersion: String = "—",
        val providerInfo: RootInfo? = null,
        val capability: CapabilityInfo = CapabilityInfo.empty(),
        val modulesActive: Int = 0,
        val scanning: Boolean = false,
        val securityProtected: Boolean = true,
        val lastError: String? = null,
        val rootTestResult: RootTestResult? = null,
        val rootTesting: Boolean = false,
        val authorizedApps: List<AuthorizedApp> = emptyList(),
        val diagnosticReport: String? = null,
        val exportingReport: Boolean = false,
        val deviceProfile: com.astraveil.core.device.DeviceProfile = com.astraveil.core.device.DeviceProfile.empty(),
        val compatibilityResult: com.astraveil.core.compatibility.CompatibilityResult = com.astraveil.core.compatibility.CompatibilityResult.unknown(),
        val runtimeStatus: RuntimeStatus = RuntimeStatus.offline(),
        val providerStatus: ProviderStatus = ProviderStatus.none(),
        val capabilityScores: List<CapabilityStatus> = emptyList(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // ---- Root access state ----
    private val _rootAccessStatus = MutableStateFlow<com.astraveil.app.root.RootAccessStatus?>(null)
    val rootAccessStatus: StateFlow<com.astraveil.app.root.RootAccessStatus?> = _rootAccessStatus.asStateFlow()

    private val _requestingAccess = MutableStateFlow(false)
    val requestingAccess: StateFlow<Boolean> = _requestingAccess.asStateFlow()

    // No hardcoded app list — refreshAppsList() queries PackageManager for
    // real installed user apps.

    fun requestRootAccess() {
        if (_requestingAccess.value) return
        viewModelScope.launch {
            _requestingAccess.value = true
            _rootAccessStatus.value = null
            val status = withContext(Dispatchers.IO) {
                val info = runCatching {
                    com.astraveil.providers.ProviderRegistry.detectActive()
                }.getOrNull()
                val provider = info?.let {
                    com.astraveil.providers.ProviderRegistry.byId(it.providerName)
                } ?: return@withContext com.astraveil.app.root.RootAccessStatus.NO_BACKEND

                com.astraveil.app.root.RootAccessManager.requestAccess(provider)
            }
            _requestingAccess.value = false
            _rootAccessStatus.value = status

            if (status == com.astraveil.app.root.RootAccessStatus.GRANTED) {
                refresh()
                refreshAppsList()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(scanning = true, lastError = null) }
            try {
                val capability = runCatching {
                    com.astraveil.core.capability.CapabilityEngine().scan()
                }.getOrNull() ?: CapabilityInfo.empty()

                val detected = runCatching {
                    com.astraveil.providers.ProviderRegistry.detectActive()
                }.getOrNull()

                val deviceProfile = runCatching {
                    com.astraveil.core.device.DeviceInspector().inspect()
                }.getOrNull() ?: com.astraveil.core.device.DeviceProfile.empty()

                val compatibilityResult = runCatching {
                    com.astraveil.core.compatibility.CompatibilityEngine().evaluate(deviceProfile)
                }.getOrNull() ?: com.astraveil.core.compatibility.CompatibilityResult.unknown()

                // Runtime intelligence from RuntimeRepository
                val runtimeStatus = runCatching { runtimeRepo.runtimeStatus() }.getOrNull()
                    ?: RuntimeStatus.offline()
                val providerStatus = runCatching { runtimeRepo.providerStatus() }.getOrNull()
                    ?: ProviderStatus.none()
                val capabilityScores = runCatching { runtimeRepo.capabilities() }.getOrNull()
                    ?: emptyList()

                _uiState.update { current ->
                    current.copy(
                        scanning = false,
                        capability = capability,
                        providerName = detected?.displayName ?: "None",
                        providerVersion = detected?.version ?: "—",
                        providerInfo = detected,
                        deviceProfile = deviceProfile,
                        compatibilityResult = compatibilityResult,
                        runtimeStatus = runtimeStatus,
                        providerStatus = providerStatus,
                        capabilityScores = capabilityScores,
                    )
                }
                refreshAppsList()
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(scanning = false, lastError = t.message)
                }
            }
        }
    }

    fun toggleAppPermission(packageName: String, authorize: Boolean) {
        viewModelScope.launch {
            try {
                val core = com.astraveil.app.AstraVeilApplication.core
                core.updatePermission(packageName, "su", authorize)
                if (authorize) {
                    core.updatePermission(packageName, "namespace", true)
                } else {
                    core.updatePermission(packageName, "namespace", false)
                }
                refreshAppsList()
            } catch (t: Throwable) {
                _uiState.update { it.copy(lastError = t.message) }
            }
        }
    }

    private fun refreshAppsList() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val installed = if (android.os.Build.VERSION.SDK_INT >= 33) {
                    pm.getInstalledApplications(
                        android.content.pm.PackageManager.ApplicationInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledApplications(0)
                }

                val core = runCatching {
                    com.astraveil.app.AstraVeilApplication.core
                }.getOrNull()

                installed
                    .filter { it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 }
                    .map { info ->
                        val hasSu = core?.permissionEngine
                            ?.canExecute(info.packageName, "su") ?: false
                        AuthorizedApp(
                            packageName = info.packageName,
                            displayName = pm.getApplicationLabel(info).toString(),
                            description = info.packageName,
                            rootAuthorized = hasSu,
                            isSystem = false,
                        )
                    }
                    .sortedBy { it.displayName.lowercase() }
            }
            _uiState.update { it.copy(authorizedApps = apps) }
        }
    }

    fun generateAndExportDiagnosticReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(exportingReport = true) }
            try {
                val exporter = com.astraveil.core.diagnostics.ReportExporter(getApplication())
                val providerName = uiState.value.providerName
                val providerVersion = uiState.value.providerVersion
                val reportText = exporter.generateReport(providerName, providerVersion)
                exporter.exportToFile(providerName, providerVersion)
                _uiState.update { it.copy(exportingReport = false, diagnosticReport = reportText) }
            } catch (t: Throwable) {
                _uiState.update { it.copy(exportingReport = false, lastError = t.message) }
            }
        }
    }

    /**
     * Test root capability by running a battery of diagnostic probes
     * (`id`, `getenforce`, `uname -r`, `ls /data/adb`, `which su`, `mount`)
     * through a [com.astraveil.app.execution.TrustedInteractiveSession] opened
     * against the active root provider with source [SessionSource.ROOT_TEST].
     *
     * P1-12: the six probes are interactive privileged commands and MUST
     * go through the same gated + audited path as the Terminal — they
     * are NOT module execution and must not bypass the audit trail. The
     * session is opened, approved programmatically (the user already
     * tapped "Test Root" — that is the explicit approval gesture), used
     * to run every probe, and closed immediately afterwards.
     */
    fun testRootCapability() {
        viewModelScope.launch {
            _uiState.update { it.copy(rootTesting = true) }
            try {
                val result = withContext(Dispatchers.IO) {
                    val info = runCatching {
                        com.astraveil.providers.ProviderRegistry.detectActive()
                    }.getOrNull()

                    if (info == null) {
                        return@withContext RootTestResult(
                            providerName = "None",
                            overallSuccess = false,
                            tests = listOf(
                                CapabilityTestResult(
                                    name = "Provider detection",
                                    command = "detectActive()",
                                    success = false,
                                    output = "No root provider detected",
                                )
                            ),
                        )
                    }

                    val provider = com.astraveil.providers.ProviderRegistry
                        .byId(info.providerName)

                    if (provider == null || !provider.available()) {
                        return@withContext RootTestResult(
                            providerName = info.displayName,
                            overallSuccess = false,
                            tests = listOf(
                                CapabilityTestResult(
                                    name = "Provider availability",
                                    command = "available()",
                                    success = false,
                                    output = "Provider not available",
                                )
                            ),
                        )
                    }

                    // Open an audited interactive session for the test probes.
                    // The "Test Root" button is the user's explicit approval
                    // gesture; we record APPROVED in the audit log and close
                    // the session when the probes finish.
                    val auditLogger = com.astraveil.core.execution.CommandAuditLogger(
                        getApplication()
                    )
                    val session = com.astraveil.app.execution.TrustedInteractiveSession(
                        provider = provider,
                        auditLogger = auditLogger,
                        source = com.astraveil.core.execution.SessionSource.ROOT_TEST,
                    )
                    session.approve()

                    try {
                        // Six diagnostic probes — each runs through the
                        // audited session so we exercise the same code path
                        // that real root calls would use.
                        val probes = listOf(
                            "id" to "Identity (uid)",
                            "getenforce" to "SELinux mode",
                            "uname -r" to "Kernel version",
                            "ls /data/adb" to "Magisk data dir",
                            "which su" to "su binary location",
                            "mount" to "Mount table",
                        )

                        val tests = probes.map { (cmd, label) ->
                            val execResult = runCatching { session.execute(cmd) }.getOrNull()
                            val success = execResult?.success == true
                            val rawOutput = (execResult?.stdout ?: "").ifBlank {
                                execResult?.stderr ?: ""
                            }.trim()
                            // Cap output so the UI stays readable.
                            val output = if (rawOutput.length > 200) {
                                rawOutput.take(200) + "…"
                            } else {
                                rawOutput
                            }
                            CapabilityTestResult(
                                name = label,
                                command = cmd,
                                success = success,
                                output = output,
                            )
                        }

                        // Overall verdict: the `id` probe must succeed AND its
                        // output must report uid=0 — i.e. we are actually root.
                        val idProbe = tests.firstOrNull { it.command == "id" }
                        val overallSuccess = idProbe?.success == true &&
                            idProbe.output.contains("uid=0")

                        RootTestResult(
                            providerName = provider.displayName,
                            overallSuccess = overallSuccess,
                            tests = tests,
                        )
                    } finally {
                        session.close()
                    }
                }

                _uiState.update { it.copy(rootTesting = false, rootTestResult = result) }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        rootTesting = false,
                        rootTestResult = RootTestResult(
                            providerName = "Error",
                            overallSuccess = false,
                            tests = listOf(
                                CapabilityTestResult(
                                    name = "Exception",
                                    command = "testRootCapability()",
                                    success = false,
                                    output = t.message ?: "Unknown error",
                                )
                            ),
                        )
                    )
                }
            }
        }
    }
}
