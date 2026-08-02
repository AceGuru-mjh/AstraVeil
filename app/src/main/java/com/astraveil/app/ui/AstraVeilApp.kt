package com.astraveil.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsInputComponent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.astraveil.app.BuildConfig
import com.astraveil.app.hub.AstraHubScreen
import com.astraveil.app.ui.design.AstraGlassStyle
import com.astraveil.app.ui.screens.AboutScreen
import com.astraveil.app.ui.screens.CapabilityScreen
import com.astraveil.app.ui.screens.DashboardScreen
import com.astraveil.app.ui.screens.modules.ModulesScreen
import com.astraveil.app.terminal.TerminalScreen
import com.astraveil.app.ui.screens.DiagnosticsScreen
import com.astraveil.app.ui.screens.ProviderScreen
import com.astraveil.app.ui.screens.SuperuserScreen
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

/**
 * Top-level AstraUI composable with Haze liquid glass navigation.
 *
 * The content (NavHost) is the Haze blur source; the bottom NavigationBar
 * is a hazeChild that blurs the content behind it in real-time. The
 * container is transparent so the blur shows through — no more opaque
 * white block.
 */
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

    // Fix: map sub-routes to their parent nav tab so the correct item highlights
    val selectedIndex = remember(currentRoute) {
        val idx = Destinations.list.indexOfFirst { it.route == currentRoute }
        if (idx >= 0) idx
        else when {
            currentRoute.startsWith("settings") ->
                Destinations.list.indexOfFirst { it.route == "settings" }
            currentRoute == "terminal" || currentRoute == "astrahub" ->
                Destinations.list.indexOfFirst { it.route == "superuser" }
            else -> 0
        }.coerceAtLeast(0)
    }

    val hazeState = remember { HazeState() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            // Pill-shaped liquid glass navigation bar
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .androidx.compose.foundation.layout.padding(
                        horizontal = 16.dp, vertical = 12.dp)
                    .androidx.compose.ui.draw.clip(
                        androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                    .hazeChild(
                        state = hazeState,
                        style = AstraGlassStyle,
                    ),
            ) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                ) {
                    Destinations.list.forEachIndexed { index, dest ->
                        NavigationBarItem(
                            selected = index == selectedIndex,
                            onClick = {
                                // Always navigate — removes the guard so tapping
                                // the current tab from a sub-page returns home
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
                                    imageVector = if (index == selectedIndex) dest.iconSelected else dest.icon,
                                    contentDescription = dest.label,
                                )
                            },
                            label = { Text(text = dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            ),
                        )
                    }
                }
            }
        }
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
                composable(Destinations.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) },
                    )
                }
                composable(Destinations.Capability.route) {
                    CapabilityScreen(viewModel = viewModel)
                }
                composable(Destinations.Provider.route) {
                    ProviderScreen(viewModel = viewModel)
                }
                composable(Destinations.Superuser.route) {
                    SuperuserScreen(navController = navController)
                }
                composable("terminal") {
                    TerminalScreen()
                }
                composable("astrahub") {
                    AstraHubScreen()
                }
                composable(Destinations.Modules.route) {
                    ModulesScreen()
                }
                composable(Destinations.About.route) {
                    AboutScreen()
                }
                composable(Destinations.Settings.route) {
                    SettingsScreen(onNavigate = { route -> navController.navigate(route) })
                }
                composable("settings_diagnostics") {
                    DiagnosticsScreen(viewModel = viewModel)
                }
                composable("settings_update_backup") {
                    UpdateBackupScreen()
                }
                composable("settings_preferences") {
                    PreferencesScreen()
                }
                composable("settings_security") {
                    SecuritySettingsScreen()
                }
                composable("settings_provider") {
                    ProviderSettingsScreen()
                }
                composable("settings_modules") {
                    ModulesSettingsScreen()
                }
                composable("settings_daemon") {
                    DaemonSettingsScreen()
                }
                composable("settings_developer") {
                    DeveloperSettingsScreen()
                }
                composable("settings_notifications") {
                    NotificationsScreen()
                }
                composable("settings_about") {
                    AboutScreen()
                }
            }
        }
    }
}


/**
 * Navigation destinations for AstraUI Phase 0.
 *
 * Uses a plain list of data class instances instead of sealed-class
 * data objects. The previous sealed-class + companion-object pattern
 * caused a NullPointerException at runtime: the companion object's
 * `list` was initialized before the nested `data object` singletons
 * were fully constructed, so `destination.route` threw NPE.
 */
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
    val Capability = Destination(
        route = "capability",
        label = AstraStrings.navCapability,
        icon = Icons.Outlined.SettingsInputComponent,
        iconSelected = Icons.Filled.SettingsInputComponent,
    )
    val Provider = Destination(
        route = "provider",
        label = AstraStrings.navProvider,
        icon = Icons.Outlined.Security,
        iconSelected = Icons.Filled.Security,
    )
    val Modules = Destination(
        route = "modules",
        label = AstraStrings.navModules,
        icon = Icons.Outlined.Apps,
        iconSelected = Icons.Filled.Apps,
    )
    val Superuser = Destination(
        route = "superuser",
        label = AstraStrings.navSuperuser,
        icon = Icons.Outlined.Security,
        iconSelected = Icons.Filled.Security,
    )
    val About = Destination(
        route = "about",
        label = AstraStrings.navAbout,
        icon = Icons.Outlined.Info,
        iconSelected = Icons.Filled.Info,
    )
    val Settings = Destination(
        route = "settings",
        label = AstraStrings.navSettings,
        icon = Icons.Outlined.Settings,
        iconSelected = Icons.Filled.Settings,
    )

    val list = listOf(Dashboard, Superuser, Modules, Settings)
}
