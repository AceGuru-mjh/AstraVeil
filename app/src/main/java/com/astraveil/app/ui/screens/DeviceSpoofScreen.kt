package com.astraveil.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.astraveil.app.spoof.DataConfidence
import com.astraveil.app.spoof.GpuRisk
import com.astraveil.app.spoof.SpoofIntegrityChecker
import com.astraveil.app.spoof.SpoofOptions
import com.astraveil.app.spoof.SpoofProfile
import com.astraveil.app.ui.design.AstraCard
import com.astraveil.app.ui.design.AstraGlassTopBar
import com.astraveil.app.ui.theme.AstraAccent
import com.astraveil.app.ui.theme.AstraError
import com.astraveil.app.ui.theme.AstraOnSurfaceMuted
import com.astraveil.app.ui.theme.AstraSuccess
import com.astraveil.app.ui.theme.AstraTeal
import com.astraveil.app.ui.theme.AstraWarning
import com.astraveil.app.viewmodel.DeviceSpoofViewModel
import com.astraveil.app.viewmodel.SpoofUiState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze

private val Mono = FontFamily.Monospace

@Composable
fun DeviceSpoofScreen(
    onNavigateBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: DeviceSpoofViewModel = viewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val hazeState = remember { HazeState() }
    var query by remember { mutableStateOf("") }
    var brandFilter by remember { mutableStateOf<String?>(null) }
    var expandedProfile by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadCurrentIdentity() }

    val profiles by viewModel.profilesFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val brands by viewModel.brandsFlow(context)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val domainProfiles = remember(profiles) {
        profiles.map { com.astraveil.app.spoof.SpoofProfileMapper.toDomain(it) }
    }

    val filtered = remember(query, brandFilter, state.currentPlatform, domainProfiles) {
        domainProfiles
            .filter { brandFilter == null || it.brand == brandFilter }
            .filter {
                query.isBlank() ||
                    it.name.contains(query, true) ||
                    it.model.contains(query, true) ||
                    it.device.contains(query, true)
            }
            .sortedBy {
                SpoofIntegrityChecker.gpuRisk(state.currentPlatform, it).ordinal
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AstraGlassTopBar(
            title = "Device Spoof",
            hazeState = hazeState,
            onBack = onNavigateBack,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .haze(state = hazeState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ═══ 当前身份 ═══
            item {
                Spacer(Modifier.height(8.dp))
                CurrentIdentityCard(state)
            }

            // ═══ 工具栏 ═══
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { onNavigate("profile_editor") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("＋ 新建档案")
                    }
                    androidx.compose.material3.FilledTonalButton(
                        onClick = { onNavigate("per_app_spoof") },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("按应用伪装")
                    }
                }
            }

            // ═══ 选项 ═══
            item { OptionsCard(state.options, viewModel::updateOptions) }

            // ═══ 搜索 + 品牌过滤 ═══
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索机型 / 型号 / 代号…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FilterChip(
                        selected = brandFilter == null,
                        onClick = { brandFilter = null },
                        label = { Text("全部") },
                    )
                    brands.forEach { b ->
                        FilterChip(
                            selected = brandFilter == b,
                            onClick = {
                                brandFilter = if (brandFilter == b) null else b
                            },
                            label = { Text(b) },
                        )
                    }
                }
            }

            // ═══ 档案列表 ═══
            items(filtered, key = { it.name }) { profile ->
                ProfileCard(
                    profile = profile,
                    currentPlatform = state.currentPlatform,
                    currentProps = state.currentProps,
                    isActive = state.activeProfileName == profile.name,
                    isApplying = state.applying,
                    isExpanded = expandedProfile == profile.name,
                    onToggleExpand = {
                        expandedProfile =
                            if (expandedProfile == profile.name) null
                            else profile.name
                    },
                    onApply = { viewModel.applyProfile(context, profile) },
                )
            }

            // ═══ 校验报告 ═══
            if (state.report != null) {
                item { IntegrityReportCard(state.report!!, state.verifying) }
            }

            // ═══ 恢复 ═══
            item {
                TextButton(
                    onClick = { viewModel.resetIdentity(context) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isSpoofed && !state.applying,
                ) {
                    Icon(Icons.Filled.Refresh, null)
                    Spacer(Modifier.width(4.dp))
                    Text("恢复真实身份")
                }
            }

            // ═══ 消息 ═══
            state.notice?.let { n -> item { MessageCard(n, AstraSuccess) } }
            state.error?.let { e -> item { MessageCard(e, AstraError) } }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── 当前身份卡 ──
@Composable
private fun CurrentIdentityCard(state: SpoofUiState) {
    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "当前设备身份",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (state.isSpoofed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AstraTeal.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        "已伪装：${state.activeProfileName}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = AstraTeal,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            state.currentProps["ro.build.fingerprint"]?.ifBlank { "—" } ?: "—",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Mono, fontSize = 11.sp,
            ),
            color = AstraOnSurfaceMuted,
        )
        Spacer(Modifier.height(6.dp))
        Row {
            PropTag("platform", state.currentPlatform.ifBlank { "?" })
            Spacer(Modifier.width(6.dp))
            PropTag(
                "model",
                state.currentProps["ro.product.model"]?.ifBlank { "?" } ?: "?",
            )
        }
    }
}

@Composable
private fun PropTag(key: String, value: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            "$key=$value",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Mono, fontSize = 10.sp,
            ),
            color = AstraOnSurfaceMuted,
        )
    }
}

// ── 选项卡 ──
@Composable
private fun OptionsCard(
    options: SpoofOptions,
    onUpdate: (SpoofOptions) -> Unit,
) {
    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            "伪装深度",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        OptionRow(
            "持久化（重启后保留）",
            "Magisk 使用 resetprop -p；其他后端行为需实测",
            options.persistent,
        ) { onUpdate(options.copy(persistent = it)) }
        OptionRow(
            "伪造序列号",
            "ro.serialno 按品牌格式随机生成",
            options.serial,
        ) { onUpdate(options.copy(serial = it)) }
        OptionRow(
            "重置 ANDROID_ID",
            "应用将视为全新设备（需重新登录，触发风控观察期）",
            options.androidId,
        ) { onUpdate(options.copy(androidId = it)) }
        OptionRow(
            "⚠ 修改系统版本（危险）",
            "仅当目标 Android 版本 == 当前版本时开启，否则应用可能崩溃",
            options.dangerous,
            danger = true,
        ) { onUpdate(options.copy(dangerous = it)) }
    }
}

@Composable
private fun OptionRow(
    title: String,
    hint: String,
    checked: Boolean,
    danger: Boolean = false,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (danger) AstraWarning
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = AstraOnSurfaceMuted,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = if (danger) AstraWarning else AstraTeal,
            ),
        )
    }
}

// ── 档案卡 ──
@Composable
private fun ProfileCard(
    profile: SpoofProfile,
    currentPlatform: String,
    currentProps: Map<String, String>,
    isActive: Boolean,
    isApplying: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onApply: () -> Unit,
) {
    val risk = SpoofIntegrityChecker.gpuRisk(currentPlatform, profile)
    val riskColor by animateColorAsState(
        when (risk) {
            GpuRisk.SAFE -> AstraSuccess
            GpuRisk.LOW -> AstraWarning
            GpuRisk.HIGH -> AstraError
        },
        label = "risk",
    )
    val cardAccent = if (isActive) {
        AstraTeal.copy(alpha = 0.08f)
    } else {
        Color.Transparent
    }

    AstraCard(modifier = Modifier.fillMaxWidth(), accent = cardAccent) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.PhoneAndroid, null,
                tint = if (isActive) AstraTeal else AstraOnSurfaceMuted,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(6.dp))
                    if (isActive) {
                        Icon(
                            Icons.Filled.CheckCircle, null,
                            tint = AstraTeal,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (profile.confidence == DataConfidence.MEDIUM) {
                        Icon(
                            Icons.Filled.Warning, null,
                            tint = AstraWarning,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    "${profile.model} · ${profile.device}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Mono, fontSize = 11.sp,
                    ),
                    color = AstraOnSurfaceMuted,
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PropTag("SoC", profile.soc)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(riskColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            when (risk) {
                                GpuRisk.SAFE -> "GPU 一致"
                                GpuRisk.LOW -> "GPU 同族"
                                GpuRisk.HIGH -> "GPU 风险"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = riskColor,
                        )
                    }
                }
            }
            IconButton(onClick = onToggleExpand) {
                Icon(
                    if (isExpanded) Icons.Filled.ExpandLess
                    else Icons.Filled.ExpandMore,
                    "详情",
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(260)),
            exit = shrinkVertically(animationSpec = tween(200)),
        ) {
            Column(Modifier.padding(top = 10.dp)) {
                DiffRow(
                    "ro.product.model",
                    currentProps["ro.product.model"] ?: "",
                    profile.model,
                )
                DiffRow(
                    "ro.product.brand",
                    currentProps["ro.product.brand"] ?: "",
                    profile.brand,
                )
                DiffRow(
                    "ro.product.device",
                    currentProps["ro.product.device"] ?: "",
                    profile.device,
                )
                DiffRow(
                    "ro.board.platform",
                    currentProps["ro.board.platform"] ?: "",
                    profile.platform,
                )
                DiffRow(
                    "ro.build.fingerprint",
                    currentProps["ro.build.fingerprint"] ?: "",
                    profile.fingerprint,
                )

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onApply,
                    enabled = !isApplying,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AstraAccent,
                    ),
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("应用中…")
                    } else {
                        Icon(
                            Icons.Filled.Shield, null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isActive) "重新应用" else "应用伪装")
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffRow(key: String, from: String, to: String) {
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(
            key,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Mono, fontSize = 10.sp,
            ),
            color = AstraOnSurfaceMuted,
        )
        Text(
            from.ifBlank { "—" },
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Mono,
                fontSize = 10.sp,
                textDecoration = TextDecoration.LineThrough,
            ),
            color = AstraOnSurfaceMuted.copy(alpha = 0.6f),
        )
        Text(
            to,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Mono, fontSize = 10.sp,
            ),
            color = AstraTeal,
        )
    }
}

// ── 完整性报告 ──
@Composable
private fun IntegrityReportCard(
    report: com.astraveil.app.spoof.IntegrityReport,
    verifying: Boolean,
) {
    val scoreColor = when {
        report.score >= 90 -> AstraSuccess
        report.score >= 70 -> AstraWarning
        else -> AstraError
    }
    val animatedScore by animateFloatAsState(
        report.score / 100f,
        animationSpec = tween(800),
        label = "score",
    )

    AstraCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp),
            ) {
                CircularProgressIndicator(
                    progress = { animatedScore },
                    modifier = Modifier.size(64.dp),
                    color = scoreColor,
                    strokeWidth = 6.dp,
                    trackColor = scoreColor.copy(alpha = 0.15f),
                )
                Text(
                    "${report.score}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    if (verifying) "校验中…" else "伪装完整性",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    report.verdict,
                    style = MaterialTheme.typography.bodySmall,
                    color = AstraOnSurfaceMuted,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        report.checks
            .sortedWith(compareBy({ it.passed }, { -it.weight }))
            .take(8)
            .forEach { check ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        if (check.passed) Icons.Filled.CheckCircle
                        else Icons.Filled.Warning,
                        null,
                        tint = if (check.passed) AstraSuccess else AstraError,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            check.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            check.detail,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                            ),
                            color = AstraOnSurfaceMuted,
                        )
                    }
                }
            }
    }
}

@Composable
private fun MessageCard(text: String, color: Color) {
    AstraCard(
        modifier = Modifier.fillMaxWidth(),
        accent = color.copy(alpha = 0.08f),
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}
