package com.astraveil.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astraveil.app.hub.AstraHubScreen
import com.astraveil.app.terminal.TerminalScreen
import com.astraveil.app.ui.design.AstraGlassStyle
import com.astraveil.app.ui.design.AstraShapes
import com.astraveil.app.ui.screens.AboutScreen
import com.astraveil.app.ui.screens.CapabilityScreen
import com.astraveil.app.ui.screens.DashboardScreen
import com.astraveil.app.ui.screens.DeviceSpoofScreen
import com.astraveil.app.ui.screens.DiagnosticsScreen
import com.astraveil.app.ui.screens.EnvShieldScreen
import com.astraveil.app.ui.screens.ProviderScreen
import com.astraveil.app.ui.screens.SuperuserScreen
import com.astraveil.app.ui.screens.modules.ModulesScreen
import com.astraveil.app.ui.screens.settings.DaemonSettingsScreen
import com.astraveil.app.ui.screens.settings.DeveloperSettingsScreen
import com.astraveil.app.ui.screens.settings.ModulesSettingsScreen
import com.astraveil.app.ui.screens.settings.NotificationsScreen
import com.astraveil.app.ui.screens.settings.PreferencesScreen
import com.astraveil.app.ui.screens.settings.ProviderSettingsScreen
import com.astraveil.app.ui.screens.settings.SecuritySettingsScreen
import com.astraveil.app.ui.screens.settings.UpdateBackupScreen
import com.astraveil.app.ui.settings.SettingsScreen
import com.astraveil.app.viewmodel.StatusViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/** Bottom bar height for pages to add bottom content padding. */
val LocalBottomBarInset = staticCompositionLocalOf { 0.dp }

@Composable
fun AstraVeilApp() {
    val navController = rememberNavController()
    val viewModel: StatusViewModel = viewModel()

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    androidx.compose.runtime.SideEffect {
        AstraStrings.setLocaleOverride(
            configuration.locales.takeIf { !it.isEmpty }?.get(0)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* result ignored */ }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destinations.Dashboard.route

    // ── 修复 #4：子页面路由正确映射到父级 tab ──
    val selectedIndex = remember(currentRoute) {
        val directIndex = Destinations.list.indexOfFirst { it.route == currentRoute }
        if (directIndex >= 0) {
            directIndex
        } else {
            when {
                currentRoute.startsWith("settings") ->
                    Destinations.list.indexOfFirst { it.route == "settings" }
                currentRoute == "terminal" || currentRoute == "astrahub" ->
                    Destinations.list.indexOfFirst { it.route == "dashboard" }
                currentRoute == "device_spoof" ->
                    Destinations.list.indexOfFirst { it.route == "dashboard" }
                currentRoute == "env_shield" ->
                    Destinations.list.indexOfFirst { it.route == "dashboard" }
                currentRoute == "capability" ->
                    Destinations.list.indexOfFirst { it.route == "dashboard" }
                currentRoute == "provider" ->
                    Destinations.list.indexOfFirst { it.route == "settings" }
                else -> 0
            }
        }
    }

    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            // ── 修复 #1：Pill 长条导航栏 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(AstraShapes.xl))   // 28.dp
                    .hazeChild(state = hazeState, style = AstraGlassStyle)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.04f),
                            ),
                        ),
                        shape = RoundedCornerShape(AstraShapes.xl),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // 顶部折射高光线
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 24.dp)
                        .border(
                            width = 0.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.30f),
                                    Color.Transparent,
                                ),
                            ),
                            shape = RoundedCornerShape(28.dp),
                        ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Destinations.list.forEachIndexed { index, dest ->
                        NavigationBarItem(
                            modifier = Modifier.weight(1f),
                            selected = index == selectedIndex,
                            onClick = {
                                // ── 修复 #2：去掉 route != currentRoute 守卫 ──
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (index == selectedIndex) {
                                        dest.iconSelected
                                    } else {
                                        dest.icon
                                    },
                                    contentDescription = dest.label,
                                )
                            },
                            label = {
                                Text(text = dest.label, fontSize = 11.sp)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        androidx.compose.runtime.CompositionLocalProvider(
            LocalBottomBarInset provides innerPadding.calculateBottomPadding(),
        ) {
            NavHost(
                navController = navController,
                startDestination = Destinations.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .haze(state = hazeState),
            ) {
                // ── 4 个主 tab ──
                composable(Destinations.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) },
                    )
                }
                composable(Destinations.Superuser.route) {
                    SuperuserScreen(navController = navController)
                }
                composable(Destinations.Modules.route) {
                    ModulesScreen()
                }
                composable(Destinations.Settings.route) {
                    SettingsScreen(onNavigate = { route -> navController.navigate(route) })
                }

                // ── 功能子页面（不在导航栏，但路由保留） ──
                composable("terminal") { TerminalScreen() }
                composable("astrahub") { AstraHubScreen() }
                composable("device_spoof") {
                    DeviceSpoofScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                composable("env_shield") {
                    EnvShieldScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
                // Capability/Provider 保留为可编程访问的子页面
                composable("capability") {
                    CapabilityScreen(viewModel = viewModel)
                }
                composable("provider") {
                    ProviderScreen(viewModel = viewModel)
                }

                // ── Settings 子页面 ──
                composable("settings_diagnostics") {
                    DiagnosticsScreen(viewModel = viewModel)
                }
                composable("settings_update_backup") { UpdateBackupScreen() }
                composable("settings_preferences") { PreferencesScreen() }
                composable("settings_security") { SecuritySettingsScreen() }
                composable("settings_provider") { ProviderSettingsScreen() }
                composable("settings_modules") { ModulesSettingsScreen() }
                composable("settings_daemon") { DaemonSettingsScreen() }
                composable("settings_developer") { DeveloperSettingsScreen() }
                composable("settings_notifications") { NotificationsScreen() }
                composable("settings_about") { AboutScreen() }
            }
        }
    }
}

// ── 修复 #3：导航栏只保留 4 个 tab ──
data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
)

object Destinations {
    val Dashboard = Destination(
        route = "dashboard",
        label = AstraStrings.navDashboard,
        icon = Icons.Outlined.Dashboard,
        iconSelected = Icons.Filled.Dashboard,
    )
    val Superuser = Destination(
        route = "superuser",
        label = AstraStrings.navSuperuser,
        icon = Icons.Outlined.Security,
        iconSelected = Icons.Filled.Security,
    )
    val Modules = Destination(
        route = "modules",
        label = AstraStrings.navModules,
        icon = Icons.Outlined.Apps,
        iconSelected = Icons.Filled.Apps,
    )
    val Settings = Destination(
        route = "settings",
        label = AstraStrings.navSettings,
        icon = Icons.Outlined.Settings,
        iconSelected = Icons.Filled.Settings,
    )

    // 只保留 4 个（删除 Capability、Provider、About）
    val list = listOf(Dashboard, Superuser, Modules, Settings)
}
