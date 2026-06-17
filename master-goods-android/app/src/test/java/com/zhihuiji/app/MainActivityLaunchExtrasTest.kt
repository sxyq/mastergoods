package com.zhihuiji.app

import com.zhihuiji.app.navigation.AgentLaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class MainActivityLaunchExtrasTest {

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
    fun productionRuntimeGuardsOnlyApplyToTrueReleaseBuild() {
        assertTrue(MainActivity.shouldEnforceProductionRuntimeGuards("release"))
        assertFalse(MainActivity.shouldEnforceProductionRuntimeGuards("debug"))
        assertFalse(MainActivity.shouldEnforceProductionRuntimeGuards("nonMinifiedRelease"))
        assertFalse(MainActivity.shouldEnforceProductionRuntimeGuards("benchmarkRelease"))
    }
}
