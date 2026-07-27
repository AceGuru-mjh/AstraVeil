package com.astraveil.app.ui

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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.astraveil.app.ui.screens.AboutScreen
import com.astraveil.app.ui.screens.CapabilityScreen
import com.astraveil.app.ui.screens.DashboardScreen
import com.astraveil.app.ui.screens.ModulesScreen
import com.astraveil.app.ui.screens.ProviderScreen
import com.astraveil.app.ui.settings.SettingsScreen
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

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destinations.Dashboard.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AstraTopAppBar() },
        bottomBar = {
            AstraBottomBar(currentRoute) { route ->
                navController.navigate(route) {
                    // Pop back to the start destination, saving state, so the
                    // back stack stays shallow and tab state is preserved.
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
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
            composable(Destinations.Modules.route) {
                ModulesScreen(viewModel = viewModel)
            }
            composable(Destinations.About.route) {
                AboutScreen()
            }
            composable(Destinations.Settings.route) {
                SettingsScreen(onNavigate = { route -> navController.navigate(route) })
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

@Composable
private fun AstraBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Destinations.list.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) onNavigate(destination.route)
                },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.iconSelected else destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
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
        label = "Dashboard",
        icon = Icons.Outlined.Dashboard,
        iconSelected = Icons.Filled.Dashboard,
    )
    val Capability = Destination(
        route = "capability",
        label = "Capability",
        icon = Icons.Outlined.SettingsInputComponent,
        iconSelected = Icons.Filled.SettingsInputComponent,
    )
    val Provider = Destination(
        route = "provider",
        label = "Provider",
        icon = Icons.Outlined.Security,
        iconSelected = Icons.Filled.Security,
    )
    val Modules = Destination(
        route = "modules",
        label = "Modules",
        icon = Icons.Outlined.Apps,
        iconSelected = Icons.Filled.Apps,
    )
    val About = Destination(
        route = "about",
        label = "About",
        icon = Icons.Outlined.Info,
        iconSelected = Icons.Filled.Info,
    )
    val Settings = Destination(
        route = "settings",
        label = "Settings",
        icon = Icons.Outlined.Settings,
        iconSelected = Icons.Filled.Settings,
    )

    val list = listOf(Dashboard, Capability, Provider, Modules, About, Settings)
}
