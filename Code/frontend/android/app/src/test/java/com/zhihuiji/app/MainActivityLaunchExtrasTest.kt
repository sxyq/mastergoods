package com.zhihuiji.app

import com.zhihuiji.app.navigation.AgentLaunchRequest
import com.zhihuiji.app.navigation.AuthRoutes
import com.zhihuiji.app.navigation.MainAccessUiState
import com.zhihuiji.app.navigation.MainRoutes
import com.zhihuiji.app.navigation.agentChatRoute
import com.zhihuiji.app.navigation.appStartDestination
import com.zhihuiji.app.navigation.canAccessRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class MainActivityLaunchExtrasTest {

    @Test
    fun appStartDestinationWaitsForLocalSessionRestoreWithoutComposingLogin() {
        assertNull(appStartDestination(isSessionReady = false, isLoggedIn = false))
        assertNull(appStartDestination(isSessionReady = false, isLoggedIn = true))
        assertEquals(MainRoutes.MAIN, appStartDestination(isSessionReady = true, isLoggedIn = true))
        assertEquals(AuthRoutes.LOGIN, appStartDestination(isSessionReady = true, isLoggedIn = false))
    }

    @Test
    fun parseStartupAgentLaunchReturnsNullWhenNoRelevantValuesExist() {
        assertNull(
            MainActivity.parseStartupAgentLaunchValues(
                openChat = false,
                initialQuestion = null,
                conversationId = null,
            )
        )
        assertNull(
            MainActivity.parseStartupAgentLaunchValues(
                openChat = false,
                initialQuestion = "   ",
                conversationId = -1L,
            )
        )
    }

    @Test
    fun parseStartupAgentLaunchBuildsChatRequestFromInitialQuestion() {
        assertEquals(
            AgentLaunchRequest(
                openChat = true,
                initialQuestion = "customer receivable",
                conversationId = null,
            ),
            MainActivity.parseStartupAgentLaunchValues(
                openChat = false,
                initialQuestion = "  customer receivable  ",
                conversationId = null,
            ),
        )
    }

    @Test
    fun parseStartupAgentLaunchKeepsConversationIdWhenProvided() {
        assertEquals(
            AgentLaunchRequest(
                openChat = true,
                initialQuestion = null,
                conversationId = 42L,
            ),
            MainActivity.parseStartupAgentLaunchValues(
                openChat = true,
                initialQuestion = null,
                conversationId = 42L,
            ),
        )
    }

    @Test
    fun agentConversationRouteCarriesConversationIdInRequiredPathSegment() {
        val route = agentChatRoute(
            initialQuestion = "销售趋势 近 7 天",
            conversationId = 42L,
        )

        assertTrue(route.startsWith("agent_chat/42?initialQuestion="))
        assertFalse(route.contains("conversationId="))
        assertTrue(
            MainAccessUiState(
                isResolved = true,
                permissions = setOf("agent:view"),
            ).canAccessRoute(route)
        )
    }

    @Test
    fun productionRuntimeGuardsOnlyApplyToTrueReleaseBuild() {
        assertTrue(MainActivity.shouldEnforceProductionRuntimeGuards("release"))
        assertFalse(MainActivity.shouldEnforceProductionRuntimeGuards("debug"))
        assertFalse(MainActivity.shouldEnforceProductionRuntimeGuards("nonMinifiedRelease"))
        assertFalse(MainActivity.shouldEnforceProductionRuntimeGuards("benchmarkRelease"))
    }
}
