package com.zhihuiji.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Brush
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.MaterialTheme
import com.zhihuiji.core.designsystem.BackgroundGradientStart
import com.zhihuiji.core.designsystem.BackgroundGradientEnd
import com.zhihuiji.feature.auth.AuthViewModel
import com.zhihuiji.feature.auth.LoginScreen
import com.zhihuiji.feature.auth.RegisterScreen
import com.zhihuiji.feature.settings.SettingsScreen

object AuthRoutes {
    const val LOGIN = "login"
    const val REGISTER = "register"
}

object MainRoutes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val uiState by authViewModel.uiState.collectAsState()

    if (!uiState.isSessionReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(BackgroundGradientStart, BackgroundGradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val startDestination = if (uiState.isLoggedIn) MainRoutes.MAIN else AuthRoutes.LOGIN

    Box(modifier = Modifier.fillMaxSize()) {
        key(uiState.isLoggedIn) {
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
                    )
                }
                composable(MainRoutes.SETTINGS) {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
