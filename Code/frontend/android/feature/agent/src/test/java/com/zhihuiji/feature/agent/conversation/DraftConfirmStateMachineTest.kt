package com.zhihuiji.feature.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftConfirmStateMachineTest {

    @Test
    fun idleStateAllowsConfirm() {
        assertFalse(shouldSkipConfirm(DraftConfirmPhase.IDLE))
    }

    @Test
    fun confirmingStateBlocksDuplicateConfirm() {
        assertTrue(shouldSkipConfirm(DraftConfirmPhase.CONFIRMING))
    }

    @Test
    fun confirmedStateBlocksDuplicateConfirm() {
        assertTrue(shouldSkipConfirm(DraftConfirmPhase.CONFIRMED))
    }

    @Test
    fun failedStateAllowsRetryConfirm() {
        // FAILED 状态允许重试确认
        assertFalse(shouldSkipConfirm(DraftConfirmPhase.FAILED))
    }

    @Test
    fun rejectedStateDoesNotBlockConfirmLogic() {
        // REJECTED 后弹窗关闭，不会重复调用确认；逻辑上不阻止，但 UI 层已隐藏弹窗
        assertFalse(shouldSkipConfirm(DraftConfirmPhase.REJECTED))
    }

    @Test
    fun idleStateAllowsCancel() {
        assertFalse(shouldSkipCancel(DraftConfirmPhase.IDLE))
    }

    @Test
    fun confirmingStateBlocksCancel() {
        // 确认中不能取消
        assertTrue(shouldSkipCancel(DraftConfirmPhase.CONFIRMING))
    }

    @Test
    fun rejectedStateBlocksDuplicateCancel() {
        assertTrue(shouldSkipCancel(DraftConfirmPhase.REJECTED))
    }

    @Test
    fun confirmedStateAllowsCancelRetry() {
        // CONFIRMED 状态下允许取消（理论上不应出现此场景，但逻辑上不阻止）
        assertFalse(shouldSkipCancel(DraftConfirmPhase.CONFIRMED))
    }

    @Test
    fun draftConfirmStateInitializesWithIdlePhase() {
        val state = DraftConfirmState(
            draftId = 1L,
            draftType = "sale_order",
            title = "销售单草稿",
        )
        assertEquals(DraftConfirmPhase.IDLE, state.confirmPhase)
        assertNull(state.errorMessage)
        assertNull(state.status)
    }

    @Test
    fun draftConfirmStatePreservesStatusFromEvent() {
        val state = DraftConfirmState(
            draftId = 1L,
            draftType = "sale_order",
            title = "销售单草稿",
            status = "active",
        )
        assertEquals("active", state.status)
    }

    @Test
    fun draftConfirmPhaseHasFivePhases() {
        val phases = DraftConfirmPhase.values().map { it.name }
        assertEquals(
            listOf("IDLE", "CONFIRMING", "CONFIRMED", "REJECTED", "FAILED"),
            phases,
        )
    }
}
