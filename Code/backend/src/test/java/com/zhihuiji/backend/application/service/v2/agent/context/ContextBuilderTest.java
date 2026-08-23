package com.zhihuiji.backend.application.service.v2.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentContextCheckpointRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

/**
 * ContextBuilder 单元测试（plan 6.2 / 6.3）。
 *
 * <p>覆盖：预算分配、压缩触发阈值、检查点边界加载、owner/conversation 隔离、
 * 降级安全余量。
 */
class ContextBuilderTest {

    @Mock private AgentMessageRepository agentMessageRepository;
    @Mock private AgentContextCheckpointRepository checkpointRepository;

    private ContextBuilder builder;
    private AgentLlmProperties llmProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        llmProperties = new AgentLlmProperties();
        llmProperties.setModel("test-model");
        llmProperties.setWireApi("anthropic");
        // 已知窗口覆盖：非保守模式，便于断言预算比例；unknown model 走保守回退。
        builder = new ContextBuilder(
            agentMessageRepository,
            checkpointRepository,
            new ContextWindowResolver(
                llmProperties,
                131_072,
                Map.of("default:test-model:anthropic", 64_000)
            ),
            new TokenEstimator(),
            llmProperties
        );
    }

    private AgentMessageEntity message(long id, long conversationId, String role, String content) {
        AgentMessageEntity entity = new AgentMessageEntity();
        setId(entity, id);
        entity.setConversationId(conversationId);
        entity.setRole(role);
        entity.setContent(content);
        entity.setCreatedAt(id);
        return entity;
    }

    private AgentContextCheckpointEntity checkpoint(Long boundaryId, String summary) {
        AgentContextCheckpointEntity entity = new AgentContextCheckpointEntity();
        setId(entity, 99L);
        entity.setOwnerUserId(1L);
        entity.setConversationId(201L);
        entity.setSourceBoundaryMessageId(boundaryId);
        entity.setSummaryBody(summary);
        entity.setStatus("active");
        entity.setRevision(1);
        return entity;
    }

    private static void setId(Object target, Long id) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to set test entity id", ex);
        }
    }

    @Test
    void budgetRatiosMatchConfiguredPercentages() {
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
            eq(1L), eq(201L), any(PageRequest.class)))
            .thenReturn(List.of());

        ContextBuilder.ContextPackage context = builder.build(1L, 201L, "查一下客户张三的欠款", "工具目录", "当前作用域说明");

        ContextBuilder.ContextBudget budget = context.budget();
        // 已知窗口 64000，非保守模式，预算按默认比例分配。
        assertEquals(64_000, budget.providerWindow());
        assertEquals(64_000, budget.usableWindow());
        assertEquals((int) Math.floor(64_000 * 0.10), budget.systemBudget());
        assertEquals((int) Math.floor(64_000 * 0.03), budget.scopeBudget());
        assertEquals((int) Math.floor(64_000 * 0.08), budget.currentQuestionBudget());
        assertEquals((int) Math.floor(64_000 * 0.20), budget.toolResultBudget());
        assertEquals((int) Math.floor(64_000 * 0.15), budget.reservedOutputBudget());
        assertEquals((int) Math.floor(64_000 * 0.10), budget.safetyBudget());
        assertTrue(budget.historyBudget() > 0);
        assertFalse(budget.compactionNeeded());
        assertFalse(budget.degradedEstimate());
    }

    @Test
    void longCurrentQuestionTriggersCompactionBudget() {
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
            eq(1L), eq(201L), any(PageRequest.class)))
            .thenReturn(List.of());
        String longQuestion = "用户".repeat(50_000);

        ContextBuilder.ContextPackage context = builder.build(1L, 201L, longQuestion, "工具目录", "作用域");

        assertTrue(context.budget().compactionNeeded());
        // 当前问题必须完整保留，不能被截断。
        assertEquals(longQuestion, context.currentUserMessage());
    }

    @Test
    void activeCheckpointLoadsMessagesAfterBoundary() {
        AgentContextCheckpointEntity checkpoint = checkpoint(50L, "{\"summary_version\":1}");
        when(checkpointRepository.findActiveByOwnerAndConversation(1L, 201L))
            .thenReturn(Optional.of(checkpoint));
        List<AgentMessageEntity> after = List.of(
            message(51L, 201L, "user", "继续"),
            message(52L, 201L, "assistant", "好的")
        );
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdAndIdGreaterThanOrderByIdAsc(1L, 201L, 50L))
            .thenReturn(after);

        ContextBuilder.ContextPackage context = builder.build(1L, 201L, "继续", "工具目录", "作用域");

        assertTrue(context.hasActiveCheckpoint());
        assertEquals(50L, context.boundaryMessageId());
        assertEquals(after, context.messagesAfterBoundary());
        assertEquals("{\"summary_version\":1}", context.checkpointSummary());
        // 有检查点时不得再走最近消息查询路径。
        verify(agentMessageRepository, never())
            .findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(anyLong(), anyLong(), any(PageRequest.class));
    }

    @Test
    void withoutCheckpointLoadsRecentMessagesAscending() {
        when(checkpointRepository.findActiveByOwnerAndConversation(1L, 201L))
            .thenReturn(Optional.empty());
        // Repository 返回倒序（createdAt DESC, id DESC），ContextBuilder 反转成时间正序。
        List<AgentMessageEntity> descending = List.of(
            message(11L, 201L, "user", "最近一条"),
            message(10L, 201L, "assistant", "上一条")
        );
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
            1L, 201L, PageRequest.of(0, ContextBuilder.HISTORY_MESSAGE_LIMIT)))
            .thenReturn(descending);

        ContextBuilder.ContextPackage context = builder.build(1L, 201L, "你好", "工具目录", "作用域");

        assertFalse(context.hasActiveCheckpoint());
        assertEquals(null, context.boundaryMessageId());
        // 反转后按 id 升序：10 在 11 前。
        assertEquals(10L, context.messagesAfterBoundary().get(0).getId());
        assertEquals(11L, context.messagesAfterBoundary().get(1).getId());
    }

    @Test
    void unknownModelDegradedEstimateRaisesSafetyMargin() {
        llmProperties.setModel("unknown-future-model");
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
            anyLong(), anyLong(), any(PageRequest.class)))
            .thenReturn(List.of());

        ContextBuilder.ContextPackage context = builder.build(1L, 201L, "你好", "工具目录", "作用域");

        assertTrue(context.budget().degradedEstimate());
        // 降级时安全余量提升 10%：safetyBudget = 20% * window。
        assertEquals((int) Math.floor(8192 * (0.10 + ContextBuilder.DEGRADED_SAFETY_MARGIN_BOOST)),
            context.budget().safetyBudget());
    }

    @Test
    void ownerAndConversationArePassedToIsolatedQueries() {
        when(checkpointRepository.findActiveByOwnerAndConversation(7L, 305L))
            .thenReturn(Optional.empty());
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtDescIdDesc(
            7L, 305L, PageRequest.of(0, ContextBuilder.HISTORY_MESSAGE_LIMIT)))
            .thenReturn(List.of());

        ContextBuilder.ContextPackage context = builder.build(7L, 305L, "你好", "工具目录", "作用域");

        assertNotNull(context);
        assertEquals(7L, context.ownerUserId());
        assertEquals(305L, context.conversationId());
        verify(checkpointRepository).findActiveByOwnerAndConversation(7L, 305L);
    }
}
