package com.zhihuiji.backend.application.service.v2.agent.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextBuilder.ContextBudget;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextBuilder.ContextPackage;
import com.zhihuiji.backend.application.service.v2.agent.context.ContextCompactionService.CompactionResult;
import com.zhihuiji.backend.domain.entity.AgentContextCheckpointEntity;
import com.zhihuiji.backend.domain.entity.AgentMessageEntity;
import com.zhihuiji.backend.infrastructure.ai.LongCatAnthropicClient;
import com.zhihuiji.backend.infrastructure.config.AgentLlmProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentContextCheckpointRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * ContextCompactionService 单元测试（plan 6.4 - 6.7）。
 *
 * <p>覆盖：触发条件、确定性降级摘要、语义摘要校验、无效摘要不覆盖有效检查点、
 * 并发保存冲突回退、失效后同一边界重建（revision 提升）、owner 隔离、
 * 失效与清理委托。
 */
class ContextCompactionServiceTest {

    @Mock private AgentContextCheckpointRepository checkpointRepository;
    @Mock private LongCatAnthropicClient llmClient;

    private ContextCompactionService service;
    private ObjectMapper objectMapper;
    private AgentLlmProperties llmProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        llmProperties = new AgentLlmProperties();
        llmProperties.setModel("test-model");
        llmProperties.setWireApi("anthropic");
        service = new ContextCompactionService(
            checkpointRepository,
            llmClient,
            llmProperties,
            objectMapper,
            new TokenEstimator()
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

    /** 构造预算超限（compactionNeeded=true）的上下文包。 */
    private ContextPackage packageWithBudget(boolean compactionNeeded, List<AgentMessageEntity> messages) {
        ContextBudget budget = new ContextBudget(
            8192, 8192, 819, 245, 655, 1638, 1228, 819, 2785,
            100, 0, 50, 5000, compactionNeeded, false
        );
        return new ContextPackage(
            1L, 201L, null, null, messages,
            "history", null, "当前问题", "作用域", "工具目录", budget
        );
    }

    private List<AgentMessageEntity> twoCompletedRounds() {
        List<AgentMessageEntity> messages = new ArrayList<>();
        messages.add(message(1L, 201L, "user", "第一轮问题"));
        messages.add(message(2L, 201L, "assistant", "第一轮回答"));
        messages.add(message(3L, 201L, "tool", null));
        messages.get(2).setStructuredDataJson(
            "{\"tool_name\":\"product_catalog_lookup\",\"tool_call_id\":\"call-1\"}");
        messages.add(message(4L, 201L, "user", "第二轮问题"));
        messages.add(message(5L, 201L, "assistant", "第二轮回答"));
        messages.add(message(6L, 201L, "tool", null));
        messages.get(5).setStructuredDataJson(
            "{\"tool_name\":\"customer_receivable_lookup\",\"tool_call_id\":\"call-2\"}");
        messages.add(message(7L, 201L, "user", "当前问题"));
        return messages;
    }

    private AgentContextCheckpointEntity checkpoint(Long boundary, String summary, String status) {
        AgentContextCheckpointEntity entity = new AgentContextCheckpointEntity();
        setId(entity, 99L);
        entity.setOwnerUserId(1L);
        entity.setConversationId(201L);
        entity.setSourceBoundaryMessageId(boundary);
        entity.setSourceMessageCount(6);
        entity.setSummaryBody(summary);
        entity.setStatus(status);
        entity.setRevision(1);
        entity.setEstimatedInputTokens(10);
        entity.setEstimatedOutputTokens(0);
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
    void budgetOkReturnsNoCompactionWithoutRepositoryWrites() {
        ContextPackage context = packageWithBudget(false, twoCompletedRounds());
        CompactionResult result = service.compactIfNeeded(context);
        assertFalse(result.occurred());
        assertNull(result.checkpoint());
        verify(checkpointRepository, never()).save(any());
        verify(llmClient, never()).createJsonMessage(anyString(), anyString());
    }

    @Test
    void compactionUsesDeterministicSummaryWhenLlmUnavailable() {
        when(llmClient.isConfigured()).thenReturn(false);
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());

        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, twoCompletedRounds()));

        assertTrue(result.occurred());
        assertEquals(ContextCompactionService.QUALITY_DETERMINISTIC, result.quality());
        // 压缩了前 6 条（第一、二轮），边界为第 6 条消息。
        assertEquals(6L, result.boundaryMessageId());
        assertEquals(6, result.compactedCount());
        assertNotNull(result.summaryPreview());
        assertTrue(result.summaryPreview().contains("summary_version"));
        ArgumentCaptor<AgentContextCheckpointEntity> captor = ArgumentCaptor.forClass(AgentContextCheckpointEntity.class);
        verify(checkpointRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getOwnerUserId());
        assertEquals(201L, captor.getValue().getConversationId());
        assertEquals(6L, captor.getValue().getSourceBoundaryMessageId());
        assertEquals("active", captor.getValue().getStatus());
    }

    @Test
    void validSemanticSummaryIsUsedWithSemanticQuality() {
        when(llmClient.isConfigured()).thenReturn(true);
        ObjectNode valid = objectMapper.createObjectNode();
        valid.put("summary_version", 1);
        valid.put("conversation_goal", "查询欠款");
        valid.putArray("confirmed_facts").add("张三商贸欠款 600");
        valid.put("source_boundary_message_id", 6L);
        valid.put("source_message_count", 6);
        when(llmClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of(valid.toString()));
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());

        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, twoCompletedRounds()));

        assertTrue(result.occurred());
        assertEquals(ContextCompactionService.QUALITY_SEMANTIC, result.quality());
        assertTrue(result.summaryPreview().contains("张三商贸欠款"));
    }

    @Test
    void invalidSemanticSummaryFallsBackToDeterministicAndNeverOverwritesCommitted() {
        when(llmClient.isConfigured()).thenReturn(true);
        // 无效语义摘要：source_boundary_message_id 与真实边界不一致。
        ObjectNode invalid = objectMapper.createObjectNode();
        invalid.put("summary_version", 1);
        invalid.put("source_boundary_message_id", 999L);
        invalid.put("source_message_count", 6);
        when(llmClient.createJsonMessage(anyString(), anyString()))
            .thenReturn(Optional.of(invalid.toString()));
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());

        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, twoCompletedRounds()));

        assertTrue(result.occurred());
        assertEquals(ContextCompactionService.QUALITY_DETERMINISTIC, result.quality());
        // 保存的摘要必须是确定性摘要（含 summary_version 结构），不是无效语义 JSON。
        ArgumentCaptor<AgentContextCheckpointEntity> captor = ArgumentCaptor.forClass(AgentContextCheckpointEntity.class);
        verify(checkpointRepository).save(captor.capture());
        assertTrue(captor.getValue().getSummaryBody().contains("confirmed_facts"));
        assertFalse(captor.getValue().getSummaryBody().contains("999"));
    }

    @Test
    void providerErrorFallsBackToDeterministicSummary() {
        when(llmClient.isConfigured()).thenReturn(true);
        when(llmClient.createJsonMessage(anyString(), anyString()))
            .thenThrow(new RuntimeException("provider timeout"));
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());

        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, twoCompletedRounds()));

        assertTrue(result.occurred());
        assertEquals(ContextCompactionService.QUALITY_DETERMINISTIC, result.quality());
        // Provider 异常不能变成未处理的 500 或丢失检查点。
        assertNotNull(result.checkpoint());
    }

    @Test
    void concurrentSaveConflictReadsCommittedVersionWithoutThrowing() {
        when(llmClient.isConfigured()).thenReturn(false);
        AgentContextCheckpointEntity committed = checkpoint(6L, "committed-summary", "active");
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenThrow(new DataIntegrityViolationException("uk_agent_context_checkpoints_boundary_revision"));
        when(checkpointRepository.findActiveByOwnerAndConversation(1L, 201L))
            .thenReturn(Optional.of(committed));

        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, twoCompletedRounds()));

        // 并发冲突不抛未处理异常，回退为读取已提交版本。
        assertNotNull(result.checkpoint());
        assertEquals(committed, result.checkpoint());
        assertTrue(result.occurred());
    }

    @Test
    void invalidatedBoundaryRegeneratesWithHigherRevision() {
        when(llmClient.isConfigured()).thenReturn(false);
        // revision=1 已存在（invalidated），无有效版本可读；第二次保存（revision=2）成功。
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenThrow(new DataIntegrityViolationException("revision 1 conflict"))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());

        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, twoCompletedRounds()));

        assertTrue(result.occurred());
        ArgumentCaptor<AgentContextCheckpointEntity> captor = ArgumentCaptor.forClass(AgentContextCheckpointEntity.class);
        verify(checkpointRepository, times(2)).save(captor.capture());
        List<AgentContextCheckpointEntity> saved = captor.getAllValues();
        assertEquals(1, saved.get(0).getRevision());
        assertEquals(2, saved.get(1).getRevision());
        assertEquals(2, result.checkpoint().getRevision());
    }

    @Test
    void notEnoughCompletedTurnsReturnsNoCompaction() {
        List<AgentMessageEntity> messages = List.of(
            message(1L, 201L, "user", "问题"),
            message(2L, 201L, "assistant", "回答")
        );
        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, messages));
        assertFalse(result.occurred());
        verify(checkpointRepository, never()).save(any());
    }

    @Test
    void currentQuestionAndLatestRoundAreNeverCompacted() {
        when(llmClient.isConfigured()).thenReturn(false);
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());

        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, twoCompletedRounds()));

        // 边界 6 是倒数第二条 tool 消息；当前用户问题（id=7）与最新轮次保留。
        assertEquals(6L, result.boundaryMessageId());
        assertTrue(result.summaryPreview().contains("第二轮问题"));
    }

    @Test
    void existingCheckpointPreservedWhenBudgetSufficient() {
        AgentContextCheckpointEntity existing = checkpoint(6L, "old-valid-summary", "active");
        ContextPackage context = new ContextPackage(
            1L, 201L, existing, 6L, List.of(),
            "history", "old-valid-summary", "当前问题", "作用域", "工具目录",
            new ContextBudget(8192, 8192, 819, 245, 655, 1638, 1228, 819, 2785,
                100, 0, 50, 5000, false, false)
        );
        CompactionResult result = service.compactIfNeeded(context);
        assertFalse(result.occurred());
        assertEquals(existing, result.checkpoint());
        verify(checkpointRepository, never()).save(any());
    }

    @Test
    void nullPackageReturnsNoCompaction() {
        CompactionResult result = service.compactIfNeeded(null);
        assertFalse(result.occurred());
        assertNull(result.checkpoint());
    }

    @Test
    void invalidateAfterBoundaryDelegatesWithReason() {
        when(checkpointRepository.invalidateAfterBoundary(eq(1L), eq(201L), eq(4L), eq("message_edited"), anyLong()))
            .thenReturn(1);
        int updated = service.invalidateAfterBoundary(1L, 201L, 4L, "message_edited");
        assertEquals(1, updated);
        verify(checkpointRepository).invalidateAfterBoundary(eq(1L), eq(201L), eq(4L), eq("message_edited"), anyLong());
    }

    @Test
    void deleteByOwnerAndConversationDelegatesToRepository() {
        service.deleteByOwnerAndConversation(1L, 201L);
        verify(checkpointRepository).deleteAllByOwnerUserIdAndConversationId(1L, 201L);
    }

    @Test
    void ownerIsolationIsPreservedWhenSavingCheckpoint() {
        when(llmClient.isConfigured()).thenReturn(false);
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());
        ContextPackage context = new ContextPackage(
            42L, 777L, null, null, twoCompletedRounds(),
            "history", null, "问题", "作用域", "工具目录",
            new ContextBudget(8192, 8192, 819, 245, 655, 1638, 1228, 819, 2785,
                100, 0, 50, 5000, true, false)
        );

        CompactionResult result = service.compactIfNeeded(context);

        assertTrue(result.occurred());
        ArgumentCaptor<AgentContextCheckpointEntity> captor = ArgumentCaptor.forClass(AgentContextCheckpointEntity.class);
        verify(checkpointRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getOwnerUserId());
        assertEquals(777L, captor.getValue().getConversationId());
    }

    @Test
    void deterministicSummaryNeverContainsCredentialsLikePatterns() {
        when(llmClient.isConfigured()).thenReturn(false);
        when(checkpointRepository.save(any(AgentContextCheckpointEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(checkpointRepository.findActiveByOwnerAndConversation(anyLong(), anyLong()))
            .thenReturn(Optional.empty());

        List<AgentMessageEntity> messages = twoCompletedRounds();
        // 注入疑似敏感内容，断言摘要不携带完整手机号/凭据。
        messages.get(1).setContent("回答含手机号 13800138000 与密码 sk-abcdef123456");
        CompactionResult result = service.compactIfNeeded(packageWithBudget(true, messages));

        assertTrue(result.occurred());
        assertFalse(result.summaryPreview().contains("sk-abcdef123456"));
    }
}
