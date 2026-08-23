package com.zhihuiji.backend.application.service.v2.agent.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhihuiji.backend.application.service.v2.agent.memory.AgentMemoryService.RecalledMemory;
import com.zhihuiji.backend.domain.entity.AgentMemoryEntity;
import com.zhihuiji.backend.infrastructure.config.AgentMemoryProperties;
import com.zhihuiji.backend.infrastructure.repository.AgentMemoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

/**
 * AgentMemoryService 单元测试。
 *
 * <p>覆盖：召回（owner/store 隔离、limit 上限）、自动学习关闭不召回/不写入、
 * 敏感信息脱敏、删除隔离、按会话清理、异常不阻塞、去重更新。
 */
class AgentMemoryServiceTest {

    @Mock private AgentMemoryRepository memoryRepository;

    private AgentMemoryService service;
    private AgentMemoryProperties properties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new AgentMemoryProperties();
        properties.setEnabled(true);
        properties.setDefaultRecallLimit(5);
        properties.setMaxRecallLimit(10);
        properties.setMaxSummaryLength(500);
        properties.setMaxRecallTextLength(4000);
        service = new AgentMemoryService(memoryRepository, properties);
    }

    private AgentMemoryEntity memory(Long id, Long ownerUserId, Long storeId, String summary, Long messageId) {
        AgentMemoryEntity entity = new AgentMemoryEntity();
        try {
            java.lang.reflect.Field field = AgentMemoryEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException ignored) {
            // keep null id
        }
        entity.setOwnerUserId(ownerUserId);
        entity.setStoreId(storeId);
        entity.setMemoryType("conversation_fact");
        entity.setSummary(summary);
        entity.setRecallText(summary);
        entity.setSensitivity("normal");
        entity.setConfidence(0.3);
        entity.setStatus("active");
        entity.setSourceConversationId(301L);
        entity.setSourceMessageId(messageId);
        entity.setCreatedAt(1L);
        entity.setUpdatedAt(2L);
        return entity;
    }

    @Test
    void recallScopesByOwnerAndStoreAndClampsLimit() {
        when(memoryRepository.findActiveByOwnerAndStore(eq(1L), eq(7L), any(), any(PageRequest.class)))
            .thenReturn(List.of(memory(1L, 1L, 7L, "客户张三偏好微信付款", 101L)));
        when(memoryRepository.updateLastAccessed(any(), any(), any(), any())).thenReturn(1);

        List<RecalledMemory> memories = service.recallMemories(1L, 7L, "张三", 100);

        assertEquals(1, memories.size());
        assertTrue(memories.get(0).historical(), "召回记忆必须标记为历史记忆");
        assertEquals("客户张三偏好微信付款", memories.get(0).summary());
        // limit 超过 maxRecallLimit 时被截断。
        verify(memoryRepository).findActiveByOwnerAndStore(eq(1L), eq(7L), any(),
            eq(PageRequest.of(0, 10)));
    }

    @Test
    void recallWithoutStoreScopesByOwnerOnly() {
        when(memoryRepository.findActiveByOwner(eq(1L), any(), any(PageRequest.class)))
            .thenReturn(List.of(memory(1L, 1L, null, "记忆", 101L)));
        when(memoryRepository.updateLastAccessed(any(), any(), any(), any())).thenReturn(1);

        List<RecalledMemory> memories = service.recallMemories(1L, null, null, null);

        assertEquals(1, memories.size());
        verify(memoryRepository).findActiveByOwner(eq(1L), any(), eq(PageRequest.of(0, 5)));
    }

    @Test
    void disabledAutoLearningSkipsRecallAndWrite() {
        properties.setEnabled(false);

        assertTrue(service.recallMemories(1L, null, "q", null).isEmpty());
        service.extractMemoriesAsync(1L, null, 301L, 101L, "问题", "回答", null);

        verify(memoryRepository, never()).findActiveByOwnerAndStore(any(), any(), any(), any());
        verify(memoryRepository, never()).findActiveByOwner(any(), any(), any());
        verify(memoryRepository, never()).save(any());
    }

    @Test
    void nullOwnerRecallReturnsEmptyWithoutQuerying() {
        assertTrue(service.recallMemories(null, null, "q", null).isEmpty());
        verify(memoryRepository, never()).findActiveByOwner(any(), any(), any());
    }

    @Test
    void extractMemoriesAsyncPersistsSanitizedCandidate() {
        when(memoryRepository.findByOwnerUserIdAndSourceConversationIdOrderByCreatedAtDescIdDesc(1L, 301L))
            .thenReturn(List.of());
        when(memoryRepository.save(any(AgentMemoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.extractMemoriesAsync(1L, 7L, 301L, 101L,
            "客户电话 13800138000 喜欢微信",
            "已记录客户偏好。", null);

        // 异步线程执行需要短暂等待。
        awaitAsync();
        verify(memoryRepository).save(any(AgentMemoryEntity.class));
    }

    @Test
    void sanitizeSensitiveRedactsPhoneEmailAndId() {
        assertEquals("联系 [REDACTED] 咨询",
            AgentMemoryService.sanitizeSensitive("联系 13800138000 咨询"));
        assertEquals("邮箱 [REDACTED] 已记录",
            AgentMemoryService.sanitizeSensitive("邮箱 test@example.com 已记录"));
        assertEquals("证件 [REDACTED]",
            AgentMemoryService.sanitizeSensitive("证件 11010119900307789X"));
    }

    @Test
    void deleteMemoryIsOwnerScoped() {
        when(memoryRepository.deleteByIdAndOwnerUserId(5L, 1L)).thenReturn(1L);

        assertTrue(service.deleteMemory(1L, 5L));
        // 其他 owner 删除返回 false。
        when(memoryRepository.deleteByIdAndOwnerUserId(5L, 2L)).thenReturn(0L);
        assertFalse(service.deleteMemory(2L, 5L));
        assertFalse(service.deleteMemory(null, 5L));
        assertFalse(service.deleteMemory(1L, null));
    }

    @Test
    void getMemoryIsOwnerScoped() {
        when(memoryRepository.findByIdAndOwnerUserId(5L, 1L))
            .thenReturn(Optional.of(memory(5L, 1L, null, "记忆", 101L)));
        assertTrue(service.getMemory(1L, 5L).isPresent());
        when(memoryRepository.findByIdAndOwnerUserId(5L, 2L)).thenReturn(Optional.empty());
        assertTrue(service.getMemory(2L, 5L).isEmpty());
    }

    @Test
    void listByConversationIsOwnerScoped() {
        when(memoryRepository.findByOwnerUserIdAndSourceConversationIdOrderByCreatedAtDescIdDesc(1L, 301L))
            .thenReturn(List.of(memory(5L, 1L, null, "记忆", 101L)));
        assertEquals(1, service.listMemoriesByConversation(1L, 301L).size());
        assertTrue(service.listMemoriesByConversation(2L, 301L).isEmpty());
    }

    @Test
    void duplicateSourceMessageUpdatesInsteadOfInserting() {
        AgentMemoryEntity existing = memory(5L, 1L, 7L, "旧摘要", 101L);
        when(memoryRepository.findByOwnerUserIdAndSourceConversationIdOrderByCreatedAtDescIdDesc(1L, 301L))
            .thenReturn(List.of(existing));
        when(memoryRepository.save(any(AgentMemoryEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.extractMemoriesAsync(1L, 7L, 301L, 101L, "新问题", "新回答", null);
        awaitAsync();

        verify(memoryRepository).save(existing);
        assertEquals("新回答", existing.getSummary().substring(existing.getSummary().lastIndexOf("A: ") + 3));
    }

    @Test
    void recallFailureDoesNotPropagateToCaller() {
        when(memoryRepository.findActiveByOwnerAndStore(eq(1L), eq(7L), any(), any(PageRequest.class)))
            .thenThrow(new RuntimeException("db down"));

        // 召回异常不应上抛（调用方决定是否降级）。
        try {
            List<RecalledMemory> memories = service.recallMemories(1L, 7L, "q", null);
            assertTrue(memories.isEmpty());
        } catch (RuntimeException ex) {
            throw new AssertionError("recall must not propagate", ex);
        }
    }

    @Test
    void emptyOrBlankContentSkipsExtraction() {
        service.extractMemoriesAsync(1L, 7L, 301L, 101L, "", "", null);
        service.extractMemoriesAsync(1L, 7L, 301L, 101L, null, null, null);
        verify(memoryRepository, never()).save(any());
    }

    private static void awaitAsync() {
        try {
            Thread.sleep(150L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
