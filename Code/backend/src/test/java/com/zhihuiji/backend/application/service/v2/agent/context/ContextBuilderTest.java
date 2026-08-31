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

import com.zhihuiji.backend.domain.entity.AgentDraftEntity;
import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentContextCheckpointRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentDraftRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentMessageRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * ContextBuilder 单元测试（plan 6.2 / 6.3）。
 *
 * <p>覆盖：预算分配、压缩触发阈值、检查点边界加载、owner/conversation 隔离、
 * 降级安全余量。
 */
class ContextBuilderTest {

    @Mock private AgentMessageRepository agentMessageRepository;
    @Mock private AgentContextCheckpointRepository checkpointRepository;
    @Mock private AgentDraftRepository draftRepository;

    private ContextBuilder builder;
    private AgentLlmProperties llmProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        llmProperties = new AgentLlmProperties();
        llmProperties.setModel("test-model");
        llmProperties.setWireApi("anthropic");
        when(draftRepository.findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(anyLong(), anyLong()))
            .thenReturn(List.of());
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
            llmProperties,
            draftRepository
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
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(eq(1L), eq(201L)))
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
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(eq(1L), eq(201L)))
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
            .findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(anyLong(), anyLong());
    }

    @Test
    void withoutCheckpointLoadsAllMessagesAscending() {
        when(checkpointRepository.findActiveByOwnerAndConversation(1L, 201L))
            .thenReturn(Optional.empty());
        List<AgentMessageEntity> history = List.of(
            message(10L, 201L, "assistant", "上一条"),
            message(11L, 201L, "user", "最近一条")
        );
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(1L, 201L))
            .thenReturn(history);

        ContextBuilder.ContextPackage context = builder.build(1L, 201L, "你好", "工具目录", "作用域");

        assertFalse(context.hasActiveCheckpoint());
        assertEquals(null, context.boundaryMessageId());
        // 仓储已按时间和 id 升序返回，构建器保留完整列表。
        assertEquals(10L, context.messagesAfterBoundary().get(0).getId());
        assertEquals(11L, context.messagesAfterBoundary().get(1).getId());
        verify(agentMessageRepository).findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(1L, 201L);
    }

    @Test
    void unknownModelDegradedEstimateRaisesSafetyMargin() {
        llmProperties.setModel("unknown-future-model");
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(anyLong(), anyLong()))
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
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(7L, 305L))
            .thenReturn(List.of());

        ContextBuilder.ContextPackage context = builder.build(7L, 305L, "你好", "工具目录", "作用域");

        assertNotNull(context);
        assertEquals(7L, context.ownerUserId());
        assertEquals(305L, context.conversationId());
        verify(checkpointRepository).findActiveByOwnerAndConversation(7L, 305L);
        verify(agentMessageRepository).findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(7L, 305L);
    }

    @Test
    void longHistoryUsesTokenBudgetInsteadOfFixedMessageCount() {
        when(checkpointRepository.findActiveByOwnerAndConversation(1L, 201L))
            .thenReturn(Optional.empty());
        List<AgentMessageEntity> history = new java.util.ArrayList<>();
        for (int i = 1; i <= 40; i++) {
            history.add(message(i, 201L, i % 2 == 1 ? "user" : "assistant", "历史内容 ".repeat(2_000) + i));
        }
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(1L, 201L))
            .thenReturn(history);

        ContextBuilder.ContextPackage context = builder.build(1L, 201L, "当前问题", "工具目录", "作用域");

        assertEquals(40, context.messagesAfterBoundary().size());
        assertTrue(context.budget().historyTokens() > context.budget().historyBudget());
        assertTrue(context.budget().compactionNeeded());
    }

    @Test
    void protectedRuntimeStateAndDraftMetadataAreLoadedByOwnerAndConversation() {
        when(checkpointRepository.findActiveByOwnerAndConversation(9L, 901L))
            .thenReturn(Optional.empty());
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(9L, 901L))
            .thenReturn(List.of());
        AgentDraftEntity draft = new AgentDraftEntity();
        setId(draft, 77L);
        draft.setConversationId(901L);
        draft.setDraftType("sale_order");
        draft.setTitle("待确认销售单");
        draft.setContentJson("{\"password\":\"do-not-copy\",\"amount\":99}");
        draft.setStatus("active");
        draft.setUpdatedAt(1234L);
        when(draftRepository.findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(9L, 901L))
            .thenReturn(List.of(draft));

        ContextBuilder.ContextPackage context = builder.build(
            9L,
            901L,
            "当前问题",
            "工具目录",
            "owner=9\nstore=18\npermission=sales.read\npassword=should-redact",
            List.of(new ContextBuilder.PendingToolCall("call-7", "create_sale_order", "awaiting_confirmation"))
        );

        assertEquals(1, context.pendingToolCalls().size());
        assertEquals("create_sale_order", context.pendingToolCalls().get(0).toolName());
        assertEquals(1, context.pendingDrafts().size());
        assertEquals(77L, context.pendingDrafts().get(0).draftId());
        assertEquals("active", context.pendingDrafts().get(0).status());
        assertFalse(context.scopeDescription().contains("should-redact"));
        verify(draftRepository).findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(9L, 901L);
        verify(draftRepository, never()).findAllByOwnerUserIdAndConversationIdOrderByUpdatedAtDescIdDesc(1L, 901L);
    }

    @Test
    void persistedPendingToolMetadataIsDerivedWithoutCopyingArguments() {
        when(checkpointRepository.findActiveByOwnerAndConversation(1L, 201L))
            .thenReturn(Optional.empty());
        AgentMessageEntity assistant = message(1L, 201L, "assistant", "正在等待确认");
        assistant.setStructuredDataJson(
            "{\"tool_calls\":[{\"id\":\"call-8\",\"name\":\"create_purchase\","
                + "\"arguments\":{\"password\":\"secret\"}}]}"
        );
        when(agentMessageRepository.findAllByOwnerUserIdAndConversationIdOrderByCreatedAtAscIdAsc(1L, 201L))
            .thenReturn(List.of(assistant));

        ContextBuilder.ContextPackage context = builder.build(1L, 201L, "当前问题", "工具目录", "作用域");

        assertEquals(1, context.pendingToolCalls().size());
        assertEquals("create_purchase", context.pendingToolCalls().get(0).toolName());
        assertFalse(context.pendingToolCalls().toString().contains("password"));
        assertFalse(context.pendingToolCalls().toString().contains("secret"));
    }
}
