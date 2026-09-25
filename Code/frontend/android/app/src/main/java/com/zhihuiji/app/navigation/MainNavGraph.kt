package com.zhihuiji.app.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.zhihuiji.feature.agent.AgentChatScreen
import com.zhihuiji.feature.agent.AgentWorkbenchScreen
import com.zhihuiji.feature.agent.DraftListScreen
import com.zhihuiji.feature.agent.TaskNotificationScreen
import kotlinx.coroutines.flow.Flow

object TabRoutes {
    const val HOME = "home"
    const val AGENT = "agent"
}

object DetailRoutes {
    const val DRAFT_LIST = "draft_list"
    const val TASK_NOTIFICATION = "task_notification"
    const val AGENT_CHAT = "agent_chat/{conversationId}?initialQuestion={initialQuestion}"
}

fun taskNotificationRoute(initialTab: Int = 0) = "${DetailRoutes.TASK_NOTIFICATION}?initialTab=$initialTab"
fun agentChatRoute(initialQuestion: String? = null, conversationId: Long? = null): String {
    val encodedQuestion = java.net.URLEncoder.encode(initialQuestion ?: "", Charsets.UTF_8.name())
    return "agent_chat/${conversationId ?: -1L}?initialQuestion=$encodedQuestion"
}

private val initialTabArguments = listOf(
    navArgument("initialTab") {
        type = NavType.IntType
        defaultValue = 0
    }
)
private val agentChatArguments = listOf(
    navArgument("conversationId") {
        type = NavType.LongType
    },
    navArgument("initialQuestion") {
        type = NavType.StringType
        defaultValue = ""
    }
)

@Composable
fun MainNavGraph(
    navController: NavHostController,
    selectedIndex: Int,
    homeBottomBarScrollEvents: Flow<Float>,
    onNavigateToSettings: () -> Unit,
    accessState: MainAccessUiState,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = TabRoutes.HOME
        ) {
            permissionComposable(TabRoutes.HOME, accessState, navController) {
                RewriteHomeScreen(
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAgent = {
                        navController.navigateIfAllowed(accessState, TabRoutes.AGENT)
                    },
                )
            }
            permissionComposable(TabRoutes.AGENT, accessState, navController) {
                AgentWorkbenchScreen(
                    onNavigateToChat = { question ->
                        navController.navigateIfAllowed(accessState, agentChatRoute(question))
                    },
                    onNavigateToConversation = { conversationId ->
                        navController.navigateIfAllowed(accessState, agentChatRoute(conversationId = conversationId))
                    },
                    onNavigateToDraftList = {
                        navController.navigateIfAllowed(accessState, DetailRoutes.DRAFT_LIST)
                    },
                    onNavigateToTasks = { navController.navigateIfAllowed(accessState, taskNotificationRoute()) },
                )
            }

            // 草稿列表
            permissionComposable(DetailRoutes.DRAFT_LIST, accessState, navController) {
                DraftListScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // 任务与通知
            permissionComposable(
                route = "${DetailRoutes.TASK_NOTIFICATION}?initialTab={initialTab}",
                accessState = accessState,
                navController = navController,
                arguments = initialTabArguments
            ) { backStackEntry ->
                TaskNotificationScreen(
                    onBackClick = { navController.popBackStack() },
                    initialTab = backStackEntry.arguments?.getInt("initialTab") ?: 0,
                )
            }

            // AI 聊天页
            permissionComposable(
                route = DetailRoutes.AGENT_CHAT,
                accessState = accessState,
                navController = navController,
                arguments = agentChatArguments
            ) { backStackEntry ->
                val initialQuestion = backStackEntry.arguments?.getString("initialQuestion")?.takeIf { it.isNotBlank() }
                val conversationId = backStackEntry.arguments?.getLong("conversationId")?.takeIf { it > 0 }
                AgentChatScreen(
                    initialQuestion = initialQuestion,
                    conversationId = conversationId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDraftList = {
                        navController.navigateIfAllowed(accessState, DetailRoutes.DRAFT_LIST)
                    },
                )
            }
        }
    }
}

private fun NavGraphBuilder.permissionComposable(
    route: String,
    accessState: MainAccessUiState,
    navController: NavHostController,
    arguments: List<NamedNavArgument> = emptyList(),
    content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable(
        route = route,
        arguments = arguments,
    ) { backStackEntry ->
        if (accessState.canAccessRoute(route)) {
            content(backStackEntry)
        } else {
            PermissionDeniedScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(accessState.firstAllowedTopLevelRoute()) {
                            launchSingleTop = true
                        }
                    }
                },
            )
        }
    }
}

private fun NavHostController.navigateIfAllowed(
    accessState: MainAccessUiState,
    route: String,
) {
    if (!accessState.canAccessRoute(route)) {
        return
    }
    navigate(route) {
        launchSingleTop = true
    }
}
