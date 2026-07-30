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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.astraveil.app.ui.design.LiquidGlassNavigationBar
import com.astraveil.app.ui.design.LiquidNavItem
import com.astraveil.app.ui.screens.AboutScreen
import com.astraveil.app.ui.screens.CapabilityScreen
import com.astraveil.app.ui.screens.DashboardScreen
import com.astraveil.app.ui.screens.modules.ModulesScreen
import com.astraveil.app.ui.screens.DiagnosticsScreen
import com.astraveil.app.ui.screens.ProviderScreen
import com.astraveil.app.ui.screens.RootManagerScreen
import com.astraveil.app.ui.settings.ComingSoonScreen
import com.astraveil.app.ui.settings.SettingsScreen
import com.astraveil.app.ui.screens.update.UpdateCenterScreen
import com.astraveil.app.viewmodel.StatusViewModel

/**
 * Top-level AstraUI composable: a [Scaffold] with a sticky bottom navigation
 * bar, a top app bar branded "AstraVeil" + a small version badge, and a
 * [NavHost] hosting the four Phase-0 destinations.
 *
 * Hosted by [com.astraveil.app.MainActivity] inside the [AstraVeilTheme].
 */
@Composable
fun AstraVeilApp() {
    val navController = rememberNavController()
    val viewModel: StatusViewModel = viewModel()

    // Compose-aware locale: feed the current configuration locale into
    // AstraStrings so every string read reacts to in-flight language
    // switches (configuration change → this Composable recomposes →
    // setLocaleOverride → all children that read AstraStrings recompose).
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    androidx.compose.runtime.SideEffect {
        AstraStrings.setLocaleOverride(
            configuration.locales.takeIf { !it.isEmpty }?.get(0)
        )
    }

    // ---- Runtime permission request (Android 13+ notifications) ----
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { /* result ignored — app works without notification permission */ }
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destinations.Dashboard.route

    // Map the current route to a nav-bar index. Only the 6 primary
    // destinations appear in the liquid glass nav bar; settings sub-routes
    // keep the last selection highlighted.
    val navItems = remember {
        Destinations.list.map { dest ->
            LiquidNavItem(label = dest.label, icon = dest.icon)
        }
    }
    val selectedIndex = remember(currentRoute) {
        Destinations.list.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AstraTopAppBar() },
        bottomBar = {
            LiquidGlassNavigationBar(
                items = navItems,
                selectedIndex = selectedIndex,
                onItemSelected = { index ->
                    val route = Destinations.list[index].route
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destinations.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                RootManagerScreen(viewModel = viewModel)
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
            composable("settings_updates") {
                UpdateCenterScreen()
            }
            // --- Placeholder targets for the remaining settings entries.
            //
            // These routes are referenced by SettingsScreen but were
            // previously NOT registered, which caused
            // navController.navigate() to throw IllegalArgumentException
            // and crash the app on tap. ComingSoonScreen is a safe target.
            composable("settings_general") {
                ComingSoonScreen(title = "General")
            }
            composable("settings_security") {
                ComingSoonScreen(title = "Security")
            }
            composable("settings_provider") {
                ComingSoonScreen(title = "Provider")
            }
            composable("settings_modules") {
                ComingSoonScreen(title = "Modules")
            }
            composable("settings_daemon") {
                ComingSoonScreen(title = "Daemon")
            }
            composable("settings_developer") {
                ComingSoonScreen(title = "Developer")
            }
            composable("settings_about") {
                AboutScreen()
            }
        }
    }
}

@Composable
private fun AstraTopAppBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "AstraVeil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "v${BuildConfig.ASTRAVEIL_VERSION}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

/**
 * Legacy M3 bottom bar — replaced by [LiquidGlassNavigationBar] above.
 * Kept for reference; not used in the current render path.
 */

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

    val list = listOf(Dashboard, Capability, Provider, Superuser, Modules, Settings)
}
