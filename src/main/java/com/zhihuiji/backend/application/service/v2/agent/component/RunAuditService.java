package com.zhihuiji.backend.application.service.v2.agent.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEntity;
import com.zhihuiji.backend.domain.entity.AgentRunAuditEventEntity;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditEventRepository;
import com.zhihuiji.backend.infrastructure.repository.AgentRunAuditRepository;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Agent 运行审计与运行生命周期组件。
 *
 * <p>承担原 {@code V2AgentAiService} 中两类紧密耦合的职责：
 * <ul>
 *   <li>运行生命周期：维护 {@link ActiveAgentRun} 注册表、运行取消检测（{@link #ensureRunActive}）、
 *       SSE 事件发送前的会话/序号 enrich（{@link #prepareSend}）</li>
 *   <li>审计持久化：创建/结束 run 审计记录、异步队列化审计事件、计数与告警</li>
 * </ul>
 *
 * <p>这两类职责通过 {@code activeRuns} 共享状态紧密关联（结束审计需要 active run 的丢弃/失败计数，
 * 队列化审计事件需要 active run 的写队列），因此合并到同一组件以避免与 SSE 发送组件之间的循环依赖。
 * {@code SseStreamEmitter} 单向依赖本组件。
 */
@Component
public class RunAuditService {
    private static final int AUDIT_WRITE_THREADS = 2;
    private static final int AUDIT_WRITE_QUEUE_CAPACITY = 512;

    private final AgentRunAuditRepository agentRunAuditRepository;
    private final AgentRunAuditEventRepository agentRunAuditEventRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, ActiveAgentRun> activeRuns = new ConcurrentHashMap<>();
    private ThreadPoolExecutor auditWriteExecutor = new ThreadPoolExecutor(
        AUDIT_WRITE_THREADS,
        AUDIT_WRITE_THREADS,
        0L,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<>(AUDIT_WRITE_QUEUE_CAPACITY),
        namedThreadFactory("agent-audit-write"),
        new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public RunAuditService(
        AgentRunAuditRepository agentRunAuditRepository,
        AgentRunAuditEventRepository agentRunAuditEventRepository,
        ObjectMapper objectMapper
    ) {
        this.agentRunAuditRepository = agentRunAuditRepository;
        this.agentRunAuditEventRepository = agentRunAuditEventRepository;
        this.objectMapper = objectMapper;
    }

    @PreDestroy
    public void shutdownAuditWriteExecutor() {
        auditWriteExecutor.shutdown();
        try {
            if (!auditWriteExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                auditWriteExecutor.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            auditWriteExecutor.shutdownNow();
        }
    }

    // ===== 运行生命周期注册表 =====

    public void registerRun(ActiveAgentRun activeRun) {
        activeRuns.put(activeRun.runId(), activeRun);
    }

    public boolean registerRunIfAbsent(ActiveAgentRun activeRun) {
        return activeRuns.putIfAbsent(activeRun.runId(), activeRun) == null;
    }

    public ActiveAgentRun getActiveRun(String runId) {
        return activeRuns.get(runId);
    }

    public void removeRun(String runId) {
        activeRuns.remove(runId);
    }

    public void ensureRunActive(String runId) {
        if (runId == null) {
            return;
        }
        ActiveAgentRun activeRun = activeRuns.get(runId);
        if (activeRun != null && activeRun.cancelled()) {
            throw new AgentRunCancelledException(runId);
        }
    }

    /**
     * SSE 事件发送前的统一预处理：对非取消事件做活跃检测，并为 payload 补充
     * conversation_id / seq / event_id（仅在 active run 存在时）。
     *
     * <p>对应原 {@code sendEvent} 中操作 {@code activeRuns} 的部分。
     */
    public void prepareSend(String runId, Map<String, Object> payload, boolean cancellationEvent) {
        if (runId == null) {
            return;
        }
        if (!cancellationEvent) {
            ensureRunActive(runId);
        }
        ActiveAgentRun activeRun = activeRuns.get(runId);
        if (activeRun != null) {
            payload.putIfAbsent("conversation_id", activeRun.conversationId());
            payload.putIfAbsent("seq", activeRun.nextSeq());
            payload.putIfAbsent("event_id", runId + ":" + payload.get("seq"));
        }
    }

    // ===== 审计记录生命周期 =====

    public void createRunAudit(Long ownerUserId, Long conversationId, String runId, long startedAt) {
        AgentRunAuditEntity entity = new AgentRunAuditEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setConversationId(conversationId);
        entity.setRunId(runId);
        entity.setAuditId(auditIdFor(runId));
        entity.setTraceId(traceIdFor(runId));
        entity.setStatus("running");
        entity.setStartedAt(startedAt);
        entity.setUpdatedAt(startedAt);
        entity.setEventCount(0);
        entity.setAuditWriteDroppedCount(0);
        entity.setAuditWriteFailedCount(0);
        entity.setAuditLossy(false);
        entity.setEmittedEventCount(0);
        agentRunAuditRepository.save(entity);
    }

    public void ensureRunAuditStarted(Long ownerUserId, Long conversationId, String runId, long startedAt) {
        if (agentRunAuditRepository.findByRunId(runId).isEmpty()) {
            createRunAudit(ownerUserId, conversationId, runId, startedAt);
        }
    }

    public void finishRunAudit(
        Long ownerUserId,
        String runId,
        String status,
        String mode,
        String llmStatus,
        String planSource,
        Integer toolCount,
        String errorCode,
        String errorMessage,
        long completedAt
    ) {
        awaitRunAuditEvents(runId);
        agentRunAuditRepository.findByRunIdAndOwnerUserId(runId, ownerUserId).ifPresent(entity -> {
            entity.setStatus(status);
            entity.setMode(mode);
            entity.setLlmStatus(llmStatus);
            entity.setPlanSource(planSource);
            entity.setToolCount(toolCount);
            entity.setErrorCode(errorCode);
            entity.setErrorMessage(truncate(errorMessage, 1000));
            entity.setCompletedAt(completedAt);
            entity.setUpdatedAt(completedAt);
            entity.setEventCount(currentRunEventCount(ownerUserId, runId));
            int droppedCount = currentRunAuditDroppedCount(runId);
            int failedCount = currentRunAuditFailedCount(runId);
            entity.setAuditWriteDroppedCount(droppedCount);
            entity.setAuditWriteFailedCount(failedCount);
            entity.setAuditLossy(droppedCount > 0 || failedCount > 0);
            entity.setEmittedEventCount(currentRunEmittedEventCount(runId, entity.getEventCount()));
            agentRunAuditRepository.save(entity);
        });
    }

    // ===== 审计事件队列 =====

    public void queueRunAuditEvent(String runId, Map<String, Object> payload, String payloadJson) {
        ActiveAgentRun activeRun = activeRuns.get(runId);
        Runnable writeTask = () -> persistRunAuditEvent(runId, payload, payloadJson);
        if (activeRun == null) {
            try {
                CompletableFuture.runAsync(writeTask, auditWriteExecutor).join();
            } catch (RejectedExecutionException ex) {
                // run 已不在 activeRuns 中时无法可靠回写 drop 计数；避免重新阻塞 SSE 线程。
            }
            return;
        }
        activeRun.enqueueAuditWrite(writeTask, auditWriteExecutor);
    }

    public void awaitRunAuditEvents(String runId) {
        ActiveAgentRun activeRun = activeRuns.get(runId);
        if (activeRun != null) {
            activeRun.awaitAuditWrites();
        }
    }

    private void persistRunAuditEvent(String runId, Map<String, Object> payload, String payloadJson) {
        Object eventId = payload.get("event_id");
        Object seq = payload.get("seq");
        Object eventType = payload.get("event_type");
        if (!(eventId instanceof String eventIdText) || !(seq instanceof Number seqNumber) || !(eventType instanceof String eventTypeText)) {
            return;
        }
        AgentRunAuditEventEntity entity = new AgentRunAuditEventEntity();
        entity.setRunId(runId);
        entity.setEventId(eventIdText);
        entity.setSeq(seqNumber.intValue());
        entity.setEventType(eventTypeText);
        entity.setPayloadJson(payloadJson);
        entity.setCreatedAt(System.currentTimeMillis());
        agentRunAuditEventRepository.save(entity);
    }

    private int currentRunEventCount(Long ownerUserId, String runId) {
        return Math.toIntExact(agentRunAuditEventRepository.countByRunIdAndOwnerUserId(runId, ownerUserId));
    }

    private int currentRunAuditDroppedCount(String runId) {
        ActiveAgentRun activeRun = activeRuns.get(runId);
        return activeRun == null ? 0 : activeRun.droppedAuditEventCount();
    }

    private int currentRunAuditFailedCount(String runId) {
        ActiveAgentRun activeRun = activeRuns.get(runId);
        return activeRun == null ? 0 : activeRun.failedAuditEventCount();
    }

    private int currentRunEmittedEventCount(String runId, Integer fallback) {
        ActiveAgentRun activeRun = activeRuns.get(runId);
        if (activeRun == null) {
            return Math.max(0, fallback == null ? 0 : fallback);
        }
        return activeRun.emittedEventCount();
    }

    // ===== 审计读取辅助 =====

    public java.util.List<String> auditWarnings(AgentRunAuditEntity audit) {
        java.util.List<String> warnings = new ArrayList<>();
        int droppedCount = Math.max(0, audit.getAuditWriteDroppedCount() == null ? 0 : audit.getAuditWriteDroppedCount());
        int failedCount = Math.max(0, audit.getAuditWriteFailedCount() == null ? 0 : audit.getAuditWriteFailedCount());
        if (droppedCount > 0) {
            warnings.add("audit_events_dropped:" + droppedCount);
        }
        if (failedCount > 0) {
            warnings.add("audit_events_write_failed:" + failedCount);
        }
        return warnings;
    }

    public JsonNode parseAuditPayload(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException ex) {
            return toJsonNode(mapOf(
                "parse_error", true,
                "raw", truncate(payloadJson, 1000)
            ));
        }
    }

    public String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength));
    }

    private JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    // ===== ID 与 trace 辅助（纯函数，供 SseStreamEmitter 与主类复用） =====

    public static String toolCallId(String runId, String toolName) {
        return (StringUtils.hasText(runId) ? runId : "run") + ":" + safeText(toolName, "tool");
    }

    public static String auditIdFor(String runId) {
        return toolCallId(runId, "audit");
    }

    public static String traceIdFor(String runId) {
        return toolCallId(runId, "trace");
    }

    private static String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }

    private static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger(1);
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * 一次 Agent 运行的活跃状态：SSE emitter、事件序号、审计写队列与取消标志。
     *
     * <p>主类在 {@code chatStream / cancelRun / runChatStream} 中构造并访问其公开方法；
     * {@link RunAuditService} 通过嵌套类私有访问权调用序号/计数/队列相关方法。
     */
    public static final class ActiveAgentRun {
        private final Long ownerUserId;
        private final String runId;
        private final Long conversationId;
        private final SseEmitter emitter;
        private final AtomicInteger eventSequence = new AtomicInteger(1);
        private final AtomicInteger droppedAuditEventCount = new AtomicInteger(0);
        private final AtomicInteger failedAuditEventCount = new AtomicInteger(0);
        private final Object auditLock = new Object();
        private CompletableFuture<Void> auditWriteChain = CompletableFuture.completedFuture(null);
        private volatile boolean cancelled;
        private volatile CompletableFuture<?> future;

        public ActiveAgentRun(Long ownerUserId, String runId, Long conversationId, SseEmitter emitter) {
            this.ownerUserId = ownerUserId;
            this.runId = runId;
            this.conversationId = conversationId;
            this.emitter = emitter;
        }

        public Long ownerUserId() {
            return ownerUserId;
        }

        public String runId() {
            return runId;
        }

        public SseEmitter emitter() {
            return emitter;
        }

        private Long conversationId() {
            return conversationId;
        }

        private int nextSeq() {
            return eventSequence.getAndIncrement();
        }

        private int emittedEventCount() {
            return Math.max(0, eventSequence.get() - 1);
        }

        private void enqueueAuditWrite(Runnable writeTask, ExecutorService executor) {
            synchronized (auditLock) {
                auditWriteChain = auditWriteChain
                    .handle((ignoredResult, ignoredError) -> null)
                    .thenCompose(ignored -> submitAuditWrite(writeTask, executor));
            }
        }

        private int droppedAuditEventCount() {
            return droppedAuditEventCount.get();
        }

        private int failedAuditEventCount() {
            return failedAuditEventCount.get();
        }

        private CompletableFuture<Void> submitAuditWrite(Runnable writeTask, ExecutorService executor) {
            CompletableFuture<Void> result = new CompletableFuture<>();
            try {
                executor.execute(() -> {
                    try {
                        writeTask.run();
                    } catch (RuntimeException ignored) {
                        failedAuditEventCount.incrementAndGet();
                        // 审计事件写入失败不应阻断后续 SSE 事件或污染同一 run 的队列。
                    } finally {
                        result.complete(null);
                    }
                });
            } catch (RejectedExecutionException ex) {
                droppedAuditEventCount.incrementAndGet();
                result.complete(null);
            }
            return result;
        }

        private void awaitAuditWrites() {
            CompletableFuture<Void> currentChain;
            synchronized (auditLock) {
                currentChain = auditWriteChain;
            }
            currentChain.join();
        }

        public boolean cancelled() {
            return cancelled;
        }

        public void cancel() {
            this.cancelled = true;
        }

        public void attachFuture(CompletableFuture<?> future) {
            this.future = future;
            if (cancelled && future != null) {
                future.cancel(true);
            }
        }

        public boolean cancelFutureIfNotStarted() {
            CompletableFuture<?> currentFuture = future;
            if (currentFuture != null) {
                return currentFuture.cancel(true);
            }
            return false;
        }

        public void complete() {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 已断开的 SSE 不需要额外处理。
            }
        }
    }

    /** Agent 运行被取消时抛出，由主类在流式循环中捕获以收尾。 */
    public static final class AgentRunCancelledException extends RuntimeException {
        public AgentRunCancelledException(String runId) {
            super("Agent run cancelled: " + runId);
        }
    }
}
