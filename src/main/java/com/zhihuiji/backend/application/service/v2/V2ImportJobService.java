package com.zhihuiji.backend.application.service.v2;

import com.zhihuiji.backend.api.dto.v2.sync.V2ImportJobDtos;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.ImportJobEntity;
import com.zhihuiji.backend.infrastructure.repository.ImportJobRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class V2ImportJobService {
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_SUCCEEDED = "succeeded";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_CANCELLED = "cancelled";

    public static final String STAGE_ACCEPTED = "accepted";
    public static final String STAGE_QUEUED = "queued";
    public static final String STAGE_REPLAY_READY = "replay_ready";
    public static final String STAGE_CANCELLED = "cancelled";

    private final ImportJobRepository importJobRepository;
    private final CurrentOwnerService currentOwnerService;

    public V2ImportJobService(
        ImportJobRepository importJobRepository,
        CurrentOwnerService currentOwnerService
    ) {
        this.importJobRepository = importJobRepository;
        this.currentOwnerService = currentOwnerService;
    }

    @Transactional(readOnly = true)
    public List<V2ImportJobDtos.ImportJobResponse> list(String status) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        List<ImportJobEntity> jobs = (status == null || status.isBlank())
            ? importJobRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(ownerUserId)
            : importJobRepository.findAllByOwnerUserIdAndStatusOrderByUpdatedAtDesc(ownerUserId, normalizeStatus(status));
        List<V2ImportJobDtos.ImportJobResponse> responses = new java.util.ArrayList<>(jobs.size());
        for (ImportJobEntity job : jobs) {
            responses.add(toResponse(job));
        }
        return responses;
    }

    @Transactional(readOnly = true)
    public V2ImportJobDtos.ImportJobResponse get(Long id) {
        return toResponse(getOwnedEntity(id));
    }

    @Transactional
    public V2ImportJobDtos.ImportJobResponse create(V2ImportJobDtos.ImportJobCreateRequest request) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String idempotencyKey = normalizeOptional(request.idempotencyKey());
        if (idempotencyKey != null) {
            ImportJobEntity existing = importJobRepository.findByOwnerUserIdAndIdempotencyKey(ownerUserId, idempotencyKey).orElse(null);
            if (existing != null) {
                return toResponse(existing);
            }
        }
        long now = System.currentTimeMillis();
        ImportJobEntity entity = new ImportJobEntity();
        entity.setOwnerUserId(ownerUserId);
        entity.setRequestedByUserId(ownerUserId);
        entity.setClientId(normalizeRequired(request.clientId(), "clientId 不能为空"));
        entity.setSourceType(normalizeRequired(request.sourceType(), "sourceType 不能为空"));
        entity.setSourceUri(normalizeOptional(request.sourceUri()));
        entity.setSourceChecksum(normalizeOptional(request.sourceChecksum()));
        entity.setIdempotencyKey(idempotencyKey);
        entity.setStatus(STATUS_PENDING);
        entity.setStage(STAGE_ACCEPTED);
        entity.setRetryCount(0);
        entity.setReplayCursor(normalizeOptional(request.replayCursor()));
        entity.setSummaryJson(null);
        entity.setOptionsJson(normalizeOptional(request.optionsJson()));
        entity.setFailureCode(null);
        entity.setFailureMessage(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setStartedAt(null);
        entity.setFinishedAt(null);
        entity.setLastHeartbeatAt(null);
        return toResponse(importJobRepository.save(entity));
    }

    @Transactional
    public V2ImportJobDtos.ImportJobResponse retry(Long id, V2ImportJobDtos.ImportJobRetryRequest request) {
        ImportJobEntity entity = getOwnedEntity(id);
        if (!STATUS_FAILED.equals(entity.getStatus()) && !STATUS_CANCELLED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("只有失败或已取消的导入任务才能重试");
        }
        long now = System.currentTimeMillis();
        entity.setStatus(STATUS_PENDING);
        entity.setStage(STAGE_REPLAY_READY);
        entity.setRetryCount(entity.getRetryCount() == null ? 1 : entity.getRetryCount() + 1);
        if (request != null && request.replayCursor() != null && !request.replayCursor().isBlank()) {
            entity.setReplayCursor(request.replayCursor().trim());
        }
        entity.setFailureCode(null);
        entity.setFailureMessage(null);
        entity.setStartedAt(null);
        entity.setFinishedAt(null);
        entity.setLastHeartbeatAt(null);
        entity.setUpdatedAt(now);
        return toResponse(importJobRepository.save(entity));
    }

    @Transactional
    public V2ImportJobDtos.ImportJobResponse cancel(Long id) {
        ImportJobEntity entity = getOwnedEntity(id);
        if (STATUS_SUCCEEDED.equals(entity.getStatus())
            || STATUS_FAILED.equals(entity.getStatus())
            || STATUS_CANCELLED.equals(entity.getStatus())) {
            throw new IllegalArgumentException("只有未完成的导入任务才能取消");
        }
        long now = System.currentTimeMillis();
        entity.setStatus(STATUS_CANCELLED);
        entity.setStage(STAGE_CANCELLED);
        entity.setFinishedAt(now);
        entity.setUpdatedAt(now);
        return toResponse(importJobRepository.save(entity));
    }

    private ImportJobEntity getOwnedEntity(Long id) {
        return importJobRepository.findByIdAndOwnerUserId(id, currentOwnerService.requireCurrentOwnerUserId())
            .orElseThrow(() -> new IllegalArgumentException("导入任务不存在"));
    }

    private V2ImportJobDtos.ImportJobResponse toResponse(ImportJobEntity entity) {
        return new V2ImportJobDtos.ImportJobResponse(
            entity.getId(),
            entity.getClientId(),
            entity.getSourceType(),
            entity.getSourceUri(),
            entity.getSourceChecksum(),
            entity.getIdempotencyKey(),
            entity.getStatus(),
            entity.getStage(),
            entity.getRetryCount(),
            entity.getReplayCursor(),
            entity.getSummaryJson(),
            entity.getOptionsJson(),
            entity.getFailureCode(),
            entity.getFailureMessage(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getLastHeartbeatAt()
        );
    }

    private String normalizeRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
        return status == null ? null : status.trim().toLowerCase(Locale.ROOT);
    }
}
