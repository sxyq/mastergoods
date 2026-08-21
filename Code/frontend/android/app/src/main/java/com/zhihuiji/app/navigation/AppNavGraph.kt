package com.zhihuiji.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.MaterialTheme
import com.zhihuiji.feature.auth.AuthViewModel
import com.zhihuiji.feature.auth.LoginScreen
import com.zhihuiji.feature.auth.RegisterScreen
import com.zhihuiji.feature.settings.SettingsScreen
import com.zhihuiji.feature.settings.StaffManagementScreen
import com.zhihuiji.data.sync.SyncScheduler

object AuthRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
}

object MainRoutes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val STAFF_MANAGEMENT = "settings/staff-management"
}

@Immutable
data class AgentLaunchRequest(
    val openChat: Boolean,
    val initialQuestion: String? = null,
    val conversationId: Long? = null,
)

internal fun appStartDestination(
    isSessionReady: Boolean,
    isLoggedIn: Boolean,
): String? = when {
    !isSessionReady -> null
    isLoggedIn -> MainRoutes.MAIN
    else -> AuthRoutes.LOGIN
}

@Composable
fun AppNavGraph(
    startupAgentLaunch: AgentLaunchRequest? = null,
) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            SyncScheduler.enqueue(context)
            SyncScheduler.enqueuePeriodicSync(context)
        }
    }

    val startDestination = appStartDestination(
        isSessionReady = uiState.isSessionReady,
        isLoggedIn = uiState.isLoggedIn,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        if (startDestination == null) {
            // Local encrypted-session restoration is not a network wait. Keep the
            // system launch surface visually stable instead of composing login then
            // replacing it with the already-authenticated local home screen.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            )
        } else key(uiState.isLoggedIn) {
            NavHost(navController = navController, startDestination = startDestination) {
                composable(AuthRoutes.LOGIN) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(MainRoutes.MAIN) {
                                popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = { navController.navigate(AuthRoutes.REGISTER) },
                        viewModel = authViewModel,
                    )
                }
                composable(AuthRoutes.REGISTER) {
                    RegisterScreen(
                        onRegisterSuccess = {
                            navController.navigate(MainRoutes.MAIN) {
                                popUpTo(AuthRoutes.LOGIN) { inclusive = true }
                            }
                        },
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = authViewModel,
                    )
                }
                composable(MainRoutes.MAIN) {
                    MainScreen(
                        onNavigateToSettings = { navController.navigate(MainRoutes.SETTINGS) },
                        startupAgentLaunch = startupAgentLaunch,
                    )
                }
                composable(MainRoutes.SETTINGS) {
                    val accessViewModel: MainAccessViewModel = hiltViewModel()
                    val accessState by accessViewModel.uiState.collectAsStateWithLifecycle()
                    val canManageUsers = accessState.isResolved && accessState.hasPermission("users:manage")
                    val canManageDatabase = accessState.isResolved && accessState.hasPermission("database:manage")
                    SettingsScreen(
                        canManageUsers = canManageUsers,
                        canManageDatabase = canManageDatabase,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToStaffManagement = {
                            if (canManageUsers) {
                                navController.navigate(MainRoutes.STAFF_MANAGEMENT)
                            }
                        },
                    )
                }
                composable(MainRoutes.STAFF_MANAGEMENT) {
                    val accessViewModel: MainAccessViewModel = hiltViewModel()
                    val accessState by accessViewModel.uiState.collectAsStateWithLifecycle()
                    when {
                        !accessState.isResolved && accessState.isLoading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        accessState.hasPermission("users:manage") -> {
                            StaffManagementScreen(
                                onNavigateBack = { navController.popBackStack() },
                            )
                        }
                        else -> {
                            PermissionDeniedScreen(
                                onBack = { navController.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}
