package com.zhihuiji.feature.agent

import com.zhihuiji.core.model.v2.agent.AgentConversationDto
import com.zhihuiji.core.model.v2.agent.RecentConversationItem
import org.junit.Assert.assertEquals
import org.junit.Test

class AgentWorkbenchHistoryTest {

    @Test
    fun authoritativeConversationListReplacesEmptyWorkbenchHistory() {
        val recent = resolveWorkbenchRecentConversations(
            fallback = emptyList(),
            conversations = listOf(
                AgentConversationDto(
                    id = 42L,
                    title = "离线消息恢复验证",
                    createdAt = 1_000L,
                    updatedAt = 2_000L,
                    lastMessageAt = 3_000L,
                ),
            ),
        )

        assertEquals(1, recent.size)
        assertEquals(42L, recent.single().id)
        assertEquals("离线消息恢复验证", recent.single().title)
        assertEquals(3_000L, recent.single().lastMessageAt)
    }

    @Test
    fun successfulEmptyConversationListDoesNotFallBackToStaleWorkbenchData() {
        val recent = resolveWorkbenchRecentConversations(
            fallback = listOf(
                RecentConversationItem(
                    id = 7L,
                    title = "过期聚合数据",
                    lastMessageAt = 700L,
                    messageCount = 2,
                ),
            ),
            conversations = emptyList(),
        )

        assertEquals(emptyList<RecentConversationItem>(), recent)
    }

    @Test
    fun failedConversationListKeepsWorkbenchFallback() {
        val fallback = listOf(
            RecentConversationItem(
                id = 7L,
                title = "服务端聚合历史",
                lastMessageAt = 700L,
                messageCount = 2,
            ),
        )

        assertEquals(
            fallback,
            resolveWorkbenchRecentConversations(
                fallback = fallback,
                conversations = null,
            ),
        )
    }
}
