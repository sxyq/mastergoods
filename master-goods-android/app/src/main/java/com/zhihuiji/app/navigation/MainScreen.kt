package com.zhihuiji.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zhihuiji.core.designsystem.BottomBarDestination
import com.zhihuiji.core.designsystem.GlassScaffold

object TopLevelRoutes {
    const val HOME = "main_home"
    const val DOCUMENTS = "main_documents"
    const val ARCHIVES = "main_archives"
    const val REPORTS = "main_reports"
    const val AGENT = "main_agent"
    const val SETTINGS = "main_settings"
}

val bottomBarDestinations = listOf(
    BottomBarDestination(
        route = TopLevelRoutes.HOME,
        label = "首页",
        icon = Icons.Outlined.Home,
        selectedIcon = Icons.Filled.Home,
    ),
    BottomBarDestination(
        route = TopLevelRoutes.DOCUMENTS,
        label = "单据",
        icon = Icons.Outlined.Description,
        selectedIcon = Icons.Filled.Description,
    ),
    BottomBarDestination(
        route = TopLevelRoutes.ARCHIVES,
        label = "档案",
        icon = Icons.Outlined.FolderOpen,
        selectedIcon = Icons.Filled.FolderOpen,
    ),
    BottomBarDestination(
        route = TopLevelRoutes.REPORTS,
        label = "报表",
        icon = Icons.Outlined.BarChart,
        selectedIcon = Icons.Filled.BarChart,
    ),
    BottomBarDestination(
        route = TopLevelRoutes.AGENT,
        label = "助手",
        icon = Icons.Outlined.AutoAwesome,
        selectedIcon = Icons.Filled.AutoAwesome,
    ),
)

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedRoute = bottomBarDestinations.firstOrNull { dest ->
        currentRoute?.startsWith(dest.route) == true
    }?.route ?: TopLevelRoutes.HOME
    val showBottomBar = bottomBarDestinations.any { dest ->
        currentRoute?.startsWith(dest.route) == true
    }

    GlassScaffold(
        selectedDestination = selectedRoute,
        destinations = bottomBarDestinations,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        showBottomBar = showBottomBar,
    ) { paddingValues ->
        MainNavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            onNavigateToSettings = onNavigateToSettings,
        )
    }
}
