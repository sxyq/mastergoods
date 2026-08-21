package com.zhihuiji.backend.application.service.v2;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.LegacySQLiteImportService;
import com.zhihuiji.backend.domain.entity.ImportJobEntity;
import com.zhihuiji.backend.infrastructure.repository.ImportJobRepository;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2ImportJobWorkerService {
    private static final Logger log = LoggerFactory.getLogger(V2ImportJobWorkerService.class);
    private static final Set<String> SUPPORTED_SOURCE_TYPES = Set.of("legacy_sqlite", "kingdee_android_sqlite");
    private static final int MAX_JOBS_PER_TICK = 3;
    private static final long STALE_HEARTBEAT_THRESHOLD_MS = 10 * 60 * 1000L;

    private final ImportJobRepository importJobRepository;
    private final LegacySQLiteImportService legacySQLiteImportService;
    private final ObjectMapper objectMapper;

    public V2ImportJobWorkerService(
        ImportJobRepository importJobRepository,
        LegacySQLiteImportService legacySQLiteImportService,
        ObjectMapper objectMapper
    ) {
        this.importJobRepository = importJobRepository;
        this.legacySQLiteImportService = legacySQLiteImportService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(
        fixedDelayString = "${sync.import-jobs.worker-delay-ms:3000}",
        initialDelayString = "${sync.import-jobs.worker-initial-delay-ms:1500}"
    )
    public void processPendingJobs() {
        recoverStaleJobs();
        for (int index = 0; index < MAX_JOBS_PER_TICK; index++) {
            Long jobId = claimNextPendingJobId();
            if (jobId == null) {
                return;
            }
            executeClaimedJob(jobId);
        }
    }

    @Transactional
    public void recoverStaleJobs() {
        long threshold = System.currentTimeMillis() - STALE_HEARTBEAT_THRESHOLD_MS;
        List<ImportJobEntity> staleJobs = importJobRepository.findByStatusAndLastHeartbeatAtBefore(
            V2ImportJobService.STATUS_RUNNING,
            threshold
        );
        if (staleJobs.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ImportJobEntity entity : staleJobs) {
            entity.setStatus(V2ImportJobService.STATUS_FAILED);
            entity.setStage("failed");
            entity.setFailureCode("heartbeat_timeout");
            entity.setFailureMessage("Worker 心跳超时,任务自动标记失败");
            entity.setFinishedAt(now);
            entity.setUpdatedAt(now);
            importJobRepository.save(entity);
            log.warn("Recovered stale import job {} (lastHeartbeatAt={})", entity.getId(), entity.getLastHeartbeatAt());
        }
    }

    @Transactional
    public Long claimNextPendingJobId() {
        long now = System.currentTimeMillis();
        for (ImportJobEntity entity : importJobRepository.findTop5ByStatusOrderByUpdatedAtAscCreatedAtAscIdAsc(
            V2ImportJobService.STATUS_PENDING
        )) {
            if (!V2ImportJobService.STATUS_PENDING.equals(entity.getStatus())) {
                continue;
            }
            entity.setStatus(V2ImportJobService.STATUS_RUNNING);
            entity.setStage("importing");
            entity.setStartedAt(now);
            entity.setFinishedAt(null);
            entity.setFailureCode(null);
            entity.setFailureMessage(null);
            entity.setLastHeartbeatAt(now);
            entity.setUpdatedAt(now);
            importJobRepository.save(entity);
            return entity.getId();
        }
        return null;
    }

    public void executeClaimedJob(Long jobId) {
        ImportJobEntity job = importJobRepository.findById(jobId).orElse(null);
        if (job == null || !V2ImportJobService.STATUS_RUNNING.equals(job.getStatus())) {
            return;
        }
        try {
            LegacySQLiteImportService.ImportResult result = executeImport(job);
            markSucceeded(jobId, result);
        } catch (IllegalArgumentException exception) {
            markFailed(jobId, "invalid_request", exception);
        } catch (IllegalStateException exception) {
            markFailed(jobId, "import_failed", exception);
        } catch (Exception exception) {
            markFailed(jobId, "unexpected_error", exception);
        }
    }

    private LegacySQLiteImportService.ImportResult executeImport(ImportJobEntity job) throws Exception {
        String sourceType = normalize(job.getSourceType());
        if (!SUPPORTED_SOURCE_TYPES.contains(sourceType)) {
            throw new IllegalArgumentException("暂不支持的导入来源: " + job.getSourceType());
        }
        String sourceUri = normalize(job.getSourceUri());
        if (sourceUri == null) {
            throw new IllegalArgumentException("导入任务缺少 sourceUri");
        }
        Boolean resetOwnedData = parseResetOwnedData(job.getOptionsJson());
        return legacySQLiteImportService.importIntoExistingOwner(
            job.getOwnerUserId(),
            new LegacySQLiteImportService.ExistingOwnerImportRequest(sourceUri, resetOwnedData)
        );
    }

    private Boolean parseResetOwnedData(String optionsJson) throws Exception {
        String normalized = normalize(optionsJson);
        if (normalized == null) {
            return null;
        }
        JsonNode root = objectMapper.readTree(normalized);
        if (root == null || root.isNull()) {
            return null;
        }
        if (!root.isObject()) {
            throw new IllegalArgumentException("optionsJson 必须是 JSON 对象");
        }
        JsonNode camelNode = root.get("resetOwnedData");
        if (camelNode != null && !camelNode.isNull()) {
            return camelNode.asBoolean();
        }
        JsonNode snakeNode = root.get("reset_owned_data");
        if (snakeNode != null && !snakeNode.isNull()) {
            return snakeNode.asBoolean();
        }
        return null;
    }

    @Transactional
    public void markSucceeded(Long jobId, LegacySQLiteImportService.ImportResult result) throws Exception {
        ImportJobEntity entity = importJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("导入任务不存在"));
        long now = System.currentTimeMillis();
        entity.setStatus(V2ImportJobService.STATUS_SUCCEEDED);
        entity.setStage("completed");
        entity.setSummaryJson(objectMapper.writeValueAsString(result));
        entity.setFailureCode(null);
        entity.setFailureMessage(null);
        entity.setFinishedAt(now);
        entity.setLastHeartbeatAt(now);
        entity.setUpdatedAt(now);
        importJobRepository.save(entity);
    }

    @Transactional
    public void markFailed(Long jobId, String failureCode, Exception exception) {
        ImportJobEntity entity = importJobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("导入任务不存在"));
        long now = System.currentTimeMillis();
        entity.setStatus(V2ImportJobService.STATUS_FAILED);
        entity.setStage("failed");
        entity.setFailureCode(failureCode);
        entity.setFailureMessage(truncate(normalize(exception.getMessage()), 1024));
        entity.setFinishedAt(now);
        entity.setLastHeartbeatAt(now);
        entity.setUpdatedAt(now);
        importJobRepository.save(entity);
    }

    private String normalize(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String truncate(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
