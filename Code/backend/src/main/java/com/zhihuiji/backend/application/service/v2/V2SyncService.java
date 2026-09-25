package com.zhihuiji.backend.application.service.v2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuiji.backend.application.service.CurrentOwnerService;
import com.zhihuiji.backend.domain.entity.SyncCursorEntity;
import com.zhihuiji.backend.domain.entity.SyncChangeLogEntity;
import com.zhihuiji.backend.infrastructure.repository.SyncCursorRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncChangeLogRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncOperationLogRepository;
import com.zhihuiji.backend.infrastructure.repository.SyncTombstoneRepository;
import com.zhihuiji.backend.domain.entity.SyncTombstoneEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class V2SyncService {
    private static final int DEFAULT_PULL_LIMIT = 100;
    private static final int MAX_PULL_LIMIT = 500;
    private static final List<String> SUPPORTED_ENTITY_TYPES = List.of();
    private static final List<String> UPLOADABLE_ENTITY_TYPES = List.of();

    private final SyncCursorRepository syncCursorRepository;
    private final ObjectMapper objectMapper;
    private final CurrentOwnerService currentOwnerService;
    private final SyncOperationLogRepository syncOperationLogRepository;
    private final SyncTombstoneRepository syncTombstoneRepository;
    private final SyncChangeLogRepository syncChangeLogRepository;
    private final TransactionTemplate operationTransactionTemplate;

    public V2SyncService(
        SyncCursorRepository syncCursorRepository,
        ObjectMapper objectMapper,
        CurrentOwnerService currentOwnerService,
        SyncOperationLogRepository syncOperationLogRepository,
        SyncTombstoneRepository syncTombstoneRepository,
        SyncChangeLogRepository syncChangeLogRepository,
        PlatformTransactionManager transactionManager
    ) {
        this.syncCursorRepository = syncCursorRepository;
        this.objectMapper = objectMapper;
        this.currentOwnerService = currentOwnerService;
        this.syncOperationLogRepository = syncOperationLogRepository;
        this.syncTombstoneRepository = syncTombstoneRepository;
        this.syncChangeLogRepository = syncChangeLogRepository;
        this.operationTransactionTemplate = new TransactionTemplate(transactionManager);
        this.operationTransactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public HealthResult health() {
        return new HealthResult(
            "ok",
            "owner scoped sync ready",
            true,
            System.currentTimeMillis(),
            SUPPORTED_ENTITY_TYPES,
            UPLOADABLE_ENTITY_TYPES
        );
    }

    @Transactional(readOnly = true)
    public CursorStatus cursorStatus(String clientId) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        String normalizedClientId = normalizeClientId(clientId);
        Optional<SyncCursorEntity> cursor = syncCursorRepository.findByOwnerUserIdAndClientId(ownerUserId, normalizedClientId);
        return new CursorStatus(
            normalizedClientId,
            cursor.map(SyncCursorEntity::getLastCursor).orElse(CursorToken.initial().encode()),
            cursor.map(SyncCursorEntity::getUpdatedAt).orElse(null)
        );
    }

    @Transactional
    public CursorStatus acknowledgeCursor(String clientId, String cursorValue) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        long now = System.currentTimeMillis();
        SyncCursorEntity cursor = loadOrCreateCursor(ownerUserId, clientId);
        cursor.setLastCursor(normalizeCursor(cursorValue));
        cursor.setUpdatedAt(now);
        SyncCursorEntity saved = syncCursorRepository.save(cursor);
        return new CursorStatus(saved.getClientId(), saved.getLastCursor(), saved.getUpdatedAt());
    }

    @Transactional
    public UploadResult upload(String clientId, List<SyncChange> changes, String lastSyncCursor) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        Long storeId = currentOwnerService.requireCurrentStoreId();
        long now = System.currentTimeMillis();
        List<SyncChange> safeChanges = changes == null ? List.of() : changes;
        CursorToken nextCursor = parseCursorToken(lastSyncCursor);
        if (nextCursor.updatedAt() < now) {
            nextCursor = new CursorToken(now, nextCursor.entityType(), nextCursor.entityId());
        }
        int acceptedCount = 0;
        int failedCount = 0;
        List<String> acceptedOperationIds = new ArrayList<>();
        List<String> failedOperationIds = new ArrayList<>();
        List<SyncOperationFailure> failures = new ArrayList<>();
        List<SyncOperationResult> operationResults = new ArrayList<>();
        for (SyncChange change : safeChanges) {
            try {
                boolean[] duplicateOperation = {false};
                operationTransactionTemplate.executeWithoutResult(status -> {
                    validateChange(change);
                    requireUploadableEntityType(change.entityType());
                    if (hasOperationId(change)) {
                        // The composite primary key and ON CONFLICT clause make this
                        // reservation the single atomic idempotency check. Avoid a
                        // read-then-insert pair: it added one database round trip per
                        // operation and still had a race between the two statements.
                        int reserved = syncOperationLogRepository.reserveOperation(
                            ownerUserId,
                            storeId,
                            change.operationId(),
                            change.entityType(),
                            change.entityId(),
                            change.operation(),
                            now
                        );
                        if (reserved == 0) {
                            duplicateOperation[0] = true;
                            return;
                        }
                    }
                    applyUploadedChange(ownerUserId, storeId, change, now);
                });
                if (hasOperationId(change)) {
                    acceptedOperationIds.add(change.operationId());
                }
                acceptedCount++;
                operationResults.add(new SyncOperationResult(
                    operationId(change),
                    duplicateOperation[0] ? "duplicate" : "applied",
                    null,
                    null
                ));
                if (!duplicateOperation[0]) {
                    nextCursor = advanceCursor(nextCursor, change);
                }
            } catch (RuntimeException exception) {
                failedCount++;
                String operationId = change == null ? null : change.operationId();
                if (operationId != null && !operationId.isBlank()) {
                    failedOperationIds.add(change.operationId());
                }
                String failureCode = syncFailureCode(exception);
                failures.add(new SyncOperationFailure(
                    operationId,
                    failureCode,
                    safeFailureMessage(exception)
                ));
                SyncConflictException conflict = exception instanceof SyncConflictException value ? value : null;
                operationResults.add(new SyncOperationResult(
                    operationId,
                    "version_conflict".equals(failureCode) ? "conflict" : "rejected",
                    failureCode,
                    safeFailureMessage(exception),
                    conflict == null ? null : conflict.serverVersion(),
                    conflict == null ? List.of() : conflict.conflictFields(),
                    conflict == null ? null : conflict.serverPayload()
                ));
            }
        }
        SyncCursorEntity cursor = loadOrCreateCursor(ownerUserId, clientId);
        cursor.setLastCursor(nextCursor.encode());
        cursor.setUpdatedAt(now);
        syncCursorRepository.save(cursor);
        return new UploadResult(
            acceptedCount,
            failedCount,
            failedCount == 0 ? "applied" : "partially_applied",
            cursor.getLastCursor(),
            acceptedOperationIds,
            failedOperationIds,
            failures,
            operationResults
        );
    }

    @Transactional(readOnly = true)
    public PullResult pull(String clientId, String sinceCursor, Integer limit) {
        Long ownerUserId = currentOwnerService.requireCurrentOwnerUserId();
        Long storeId = currentOwnerService.requireCurrentStoreId();
        String normalizedClientId = normalizeClientId(clientId);
        SyncCursorEntity cursor = syncCursorRepository.findByOwnerUserIdAndClientId(ownerUserId, normalizedClientId)
            .orElse(null);
        String requestedCursor = sinceCursor == null || sinceCursor.isBlank()
            ? cursor == null ? null : cursor.getLastCursor()
            : sinceCursor;
        CursorToken effectiveCursor = parseCursorToken(requestedCursor);
        int safeLimit = normalizeLimit(limit == null ? DEFAULT_PULL_LIMIT : limit);

        String persistedCursor = normalizeCursor(requestedCursor);
        PersistedChanges persistedChanges = collectPersistedChanges(
            ownerUserId, storeId, persistedCursor, safeLimit
        );
        if (!persistedChanges.changes().isEmpty() || isSequenceCursor(persistedCursor)) {
            return pagePersistedChanges(persistedChanges, persistedCursor, safeLimit);
        }

        List<SyncChange> changes = new ArrayList<>(64);

        // Collect tombstones (deleted entities) so clients can learn about deletions
        changes.addAll(collectTombstoneChanges(ownerUserId, effectiveCursor));

        changes.sort(
            Comparator.comparingLong((SyncChange item) -> safeLong(item.updatedAt()))
                .thenComparing(item -> item.entityType() == null ? "" : item.entityType())
                .thenComparing(item -> item.entityId() == null ? "" : item.entityId())
        );
        changes.removeIf(change -> !canPullEntityType(change.entityType()));

        boolean hasMore = changes.size() > safeLimit;
        List<SyncChange> page = hasMore ? new ArrayList<>(changes.subList(0, safeLimit)) : new ArrayList<>(changes);
        CursorToken nextCursor = effectiveCursor;
        if (!page.isEmpty()) {
            nextCursor = cursorFor(page.get(page.size() - 1));
        }
        // Pull tokens are read-time pagination state; durable progress advances only after ack.
        return new PullResult(page, effectiveCursor.encode(), nextCursor.encode(), hasMore);
    }

    private PersistedChanges collectPersistedChanges(
        Long ownerUserId,
        Long storeId,
        String sinceCursor,
        int limit
    ) {
        boolean sequenceCursor = isSequenceCursor(sinceCursor);
        long cursor = sequenceCursor
            ? parseSequenceCursor(sinceCursor)
            : parseCursorToken(sinceCursor).updatedAt();
        var pageable = PageRequest.of(0, Math.min(MAX_PULL_LIMIT, limit + 1));
        List<SyncChangeLogEntity> rows = sequenceCursor
            ? syncChangeLogRepository
                .findByOwnerUserIdAndStoreIdAndSequenceNumberGreaterThanOrderBySequenceNumberAsc(
                    ownerUserId, storeId, cursor, pageable)
            : syncChangeLogRepository
                .findByOwnerUserIdAndStoreIdAndChangedAtGreaterThanEqualOrderByChangedAtAscSequenceNumberAsc(
                    ownerUserId, storeId, cursor, pageable);
        CursorToken legacyCursor = parseCursorToken(sinceCursor);
        List<SyncChange> changes = new ArrayList<>(rows.size());
        List<Long> sequences = new ArrayList<>(rows.size());
        for (SyncChangeLogEntity row : rows) {
            if (!canPullEntityType(row.getEntityType())
                || (!sequenceCursor && compareCursor(
                    safeLong(row.getChangedAt()), row.getEntityType(), row.getEntityId(), legacyCursor) <= 0)) {
                continue;
            }
            changes.add(new SyncChange(
                row.getOperationId(),
                row.getEntityType(),
                row.getEntityId(),
                row.getOperation(),
                row.getPayload(),
                row.getChangedAt(),
                row.getSyncVersion()
            ));
            sequences.add(row.getSequenceNumber());
        }
        return new PersistedChanges(changes, sequences);
    }

    private PullResult pagePersistedChanges(PersistedChanges persisted, String sinceCursor, int limit) {
        List<SyncChange> changes = persisted.changes();
        boolean hasMore = changes.size() > limit;
        List<SyncChange> page = hasMore
            ? new ArrayList<>(changes.subList(0, limit))
            : new ArrayList<>(changes);
        String effectiveCursor = sinceCursor == null || sinceCursor.isBlank() ? "seq:0" : sinceCursor;
        String nextCursor = effectiveCursor;
        if (!page.isEmpty()) {
            // Keep the public SyncChange timestamp unchanged for existing clients.
            nextCursor = "seq:" + persisted.sequences().get(page.size() - 1);
        }
        return new PullResult(page, effectiveCursor, nextCursor, hasMore);
    }

    private SyncCursorEntity loadOrCreateCursor(Long ownerUserId, String clientId) {
        String normalizedClientId = normalizeClientId(clientId);
        SyncCursorEntity cursor = syncCursorRepository.findByOwnerUserIdAndClientId(ownerUserId, normalizedClientId)
            .orElseGet(SyncCursorEntity::new);
        cursor.setOwnerUserId(ownerUserId);
        cursor.setClientId(normalizedClientId);
        if (cursor.getLastCursor() == null) {
            cursor.setLastCursor(CursorToken.initial().encode());
        }
        if (cursor.getUpdatedAt() == null) {
            cursor.setUpdatedAt(System.currentTimeMillis());
        }
        return cursor;
    }




























    private List<SyncChange> collectTombstoneChanges(Long ownerUserId, CursorToken since) {
        long sinceTimestamp = since.updatedAt();
        List<SyncTombstoneEntity> tombstones = syncTombstoneRepository.findChangedByOwnerUserId(
            ownerUserId, currentStoreId(ownerUserId), sinceTimestamp);
        List<SyncChange> rows = new ArrayList<>(tombstones.size());
        for (SyncTombstoneEntity tombstone : tombstones) {
            long changedAt = safeLong(tombstone.getDeletedAt());
            if (compareCursor(changedAt, tombstone.getEntityType(), tombstone.getEntityId(), since) <= 0) continue;
            rows.add(new SyncChange(tombstone.getEntityType(), tombstone.getEntityId(), "delete", null, changedAt));
        }
        return rows;
    }






    private boolean canPullEntityType(String entityType) {
        // 旧领域实体类型已删除；新领域类型接入后在 SUPPORTED_ENTITY_TYPES 中登记。
        return entityType != null && SUPPORTED_ENTITY_TYPES.contains(entityType);
    }

    private void requireUploadableEntityType(String entityType) {
        if (!SUPPORTED_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("unsupported entity type: " + entityType);
        }
        if (!UPLOADABLE_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalStateException("server command required for entity type: " + entityType);
        }
    }


    private long nextSyncVersion(Long currentVersion, Long baseVersion) {
        long current = currentVersion == null ? 0L : Math.max(0L, currentVersion);
        long base = baseVersion == null ? current : Math.max(0L, baseVersion);
        return Math.max(current, base) + 1L;
    }

    private void validateChange(SyncChange change) {
        if (change == null
            || change.entityType() == null || change.entityType().isBlank()
            || change.entityId() == null || change.entityId().isBlank()
            || change.operation() == null || change.operation().isBlank()) {
            throw new IllegalArgumentException("sync change is incomplete");
        }
    }

    private boolean hasOperationId(SyncChange change) {
        return change.operationId() != null && !change.operationId().isBlank();
    }

    private String operationId(SyncChange change) {
        return change == null ? null : change.operationId();
    }

    private String syncFailureCode(RuntimeException exception) {
        if (exception instanceof AccessDeniedException) {
            return "permission_denied";
        }
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.startsWith("sync version conflict:")) {
            return "version_conflict";
        }
        if (message.startsWith("unsupported entity type:")) {
            return "unsupported_entity_type";
        }
        if (message.startsWith("server command required for entity type:")) {
            return "server_command_required";
        }
        if (message.contains("required")) {
            return "validation_failed";
        }
        return "sync_apply_failed";
    }

    private String safeFailureMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "sync operation failed" : message;
    }

    private void applyUploadedChange(Long ownerUserId, Long storeId, SyncChange change, long changedAt) {
        // 旧领域实体写入已删除；此处保留通用机制（删除墓碑 + 变更日志），
        // 新领域类型分发在 requireUploadableEntityType 通过后接入。
        if (isDelete(change.operation())) {
            SyncTombstoneEntity tombstone = new SyncTombstoneEntity();
            tombstone.setOwnerUserId(ownerUserId);
            tombstone.setStoreId(storeId);
            tombstone.setEntityType(change.entityType());
            tombstone.setEntityId(change.entityId());
            tombstone.setDeletedAt(System.currentTimeMillis());
            syncTombstoneRepository.save(tombstone);
        }
        JsonNode uploadedPayload = readPayload(change.payload());
        SyncChangeLogEntity log = new SyncChangeLogEntity();
        log.setOwnerUserId(ownerUserId);
        log.setStoreId(storeId);
        log.setEntityType(change.entityType());
        log.setEntityId(change.entityId());
        log.setOperation(change.operation());
        log.setPayload(change.payload());
        log.setSyncVersion(uploadedPayload.path("sync_version").isNumber()
            ? uploadedPayload.path("sync_version").asLong()
            : null);
        log.setOperationId(change.operationId());
        log.setChangedAt(changedAt);
        syncChangeLogRepository.save(log);
    }

























    private SyncChange change(String entityType, Long entityId, long updatedAt, String payload) {
        return new SyncChange(entityType, String.valueOf(entityId), "upsert", payload, updatedAt);
    }

    private boolean shouldSkip(String entityType, Long id, long changedAt, CursorToken since) {
        if (id == null) {
            return true;
        }
        return compareCursor(changedAt, entityType, String.valueOf(id), since) <= 0;
    }

    private String payload(Object... entries) {
        Map<String, Object> map = new HashMap<>(Math.max(4, entries.length / 2));
        for (int index = 0; index < entries.length; index += 2) {
            map.put((String) entries[index], entries[index + 1]);
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("failed to serialize sync payload", error);
        }
    }

    private JsonNode readPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("invalid sync payload", error);
        }
    }

    private String normalizeClientId(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return "anonymous";
        }
        return clientId.trim();
    }

    private String normalizeCursor(String cursor) {
        if (isSequenceCursor(cursor)) {
            return "seq:" + parseSequenceCursor(cursor);
        }
        return parseCursorToken(cursor).encode();
    }

    private boolean isSequenceCursor(String cursor) {
        return cursor != null && cursor.startsWith("seq:");
    }

    private long parseSequenceCursor(String cursor) {
        if (!isSequenceCursor(cursor)) {
            return 0L;
        }
        try {
            return Math.max(0L, Long.parseLong(cursor.substring("seq:".length())));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private Long currentStoreId(Long ownerUserId) {
        return currentOwnerService.findCurrentStoreId().orElseThrow(
            () -> new AccessDeniedException("当前账号没有有效门店上下文"));
    }

    private CursorToken parseCursorToken(String cursor) {
        return CursorToken.parse(cursor);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_PULL_LIMIT;
        }
        return Math.min(limit, MAX_PULL_LIMIT);
    }

    private long resolveChangedAt(Long updatedAt, Long createdAt) {
        long updated = safeLong(updatedAt);
        return updated > 0 ? updated : safeLong(createdAt);
    }

    private int compareCursor(long updatedAt, String entityType, String entityId, CursorToken cursor) {
        int timestampOrder = Long.compare(updatedAt, cursor.updatedAt());
        if (timestampOrder != 0) {
            return timestampOrder;
        }
        int entityTypeOrder = safeText(entityType).compareTo(safeText(cursor.entityType()));
        if (entityTypeOrder != 0) {
            return entityTypeOrder;
        }
        return safeText(entityId).compareTo(safeText(cursor.entityId()));
    }

    private CursorToken cursorFor(SyncChange change) {
        return new CursorToken(
            safeLong(change.updatedAt()),
            safeText(change.entityType()),
            safeText(change.entityId())
        );
    }

    private CursorToken advanceCursor(CursorToken current, SyncChange change) {
        CursorToken candidate = cursorFor(change);
        return compareCursor(candidate.updatedAt(), candidate.entityType(), candidate.entityId(), current) > 0
            ? candidate
            : current;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private Long parseEntityId(String entityId) {
        try {
            return Long.parseLong(entityId);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("invalid entity id: " + entityId, error);
        }
    }

    private boolean isDelete(String operation) {
        return "delete".equalsIgnoreCase(operation);
    }

    private String readText(JsonNode payload, String field, String currentValue, String defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            String value = node.asText();
            if (!value.isBlank()) {
                return value;
            }
        }
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue;
        }
        return defaultValue;
    }

    private String readNullableText(JsonNode payload, String field, String currentValue) {
        JsonNode node = payload.path(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isMissingNode()) {
            String value = node.asText();
            return value.isBlank() ? null : value;
        }
        return currentValue;
    }

    private Integer readInt(JsonNode payload, String field, Integer currentValue, Integer defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asInt();
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private Boolean readBoolean(JsonNode payload, String field, Boolean currentValue, Boolean defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asBoolean();
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private Double readDouble(JsonNode payload, String field, Double currentValue, Double defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            if (!node.isNumber()) {
                throw new IllegalArgumentException("同步字段不是数字: " + field);
            }
            double value = node.asDouble();
            if (!Double.isFinite(value) || (isNonNegativeNumericField(field) && value < 0.0)) {
                throw new IllegalArgumentException("同步字段数值不合法: " + field);
            }
            return value;
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private Double readNullableDouble(JsonNode payload, String field, Double currentValue) {
        JsonNode node = payload.path(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isMissingNode()) {
            if (!node.isNumber()) {
                throw new IllegalArgumentException("同步字段不是数字: " + field);
            }
            double value = node.asDouble();
            if (!Double.isFinite(value) || (isNonNegativeNumericField(field) && value < 0.0)) {
                throw new IllegalArgumentException("同步字段数值不合法: " + field);
            }
            return value;
        }
        return currentValue;
    }

    private boolean isNonNegativeNumericField(String field) {
        return Set.of(
            "balance", "sale_price", "purchase_price", "stock", "safe_stock",
            "subtotal_amount", "discount_amount", "total_amount", "paid_amount",
            "received_amount", "quantity", "unit_price", "unit_cost", "amount",
            "fee", "refund_amount", "last_purchase_price"
        ).contains(field);
    }

    private Long readLong(JsonNode payload, String field, Long currentValue, Long defaultValue) {
        JsonNode node = payload.path(field);
        if (!node.isMissingNode() && !node.isNull()) {
            return node.asLong();
        }
        if (currentValue != null) {
            return currentValue;
        }
        return defaultValue;
    }

    private Long readNullableLong(JsonNode payload, String field, Long currentValue) {
        JsonNode node = payload.path(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isMissingNode()) {
            if (node.isTextual() && node.asText().isBlank()) {
                return null;
            }
            return node.asLong();
        }
        return currentValue;
    }

    private Long readRequiredLong(JsonNode payload, String field, String message) {
        Long value = readNullableLong(payload, field, null);
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }













    public record HealthResult(
        String status,
        String message,
        Boolean ownerScoped,
        Long serverTime,
        List<String> supportedEntityTypes,
        List<String> uploadableEntityTypes
    ) {}

    public record SyncChange(
        String operationId,
        String entityType,
        String entityId,
        String operation,
        String payload,
        Long updatedAt,
        Long baseVersion
    ) {
        public SyncChange(String entityType, String entityId, String operation, String payload, Long updatedAt) {
            this(null, entityType, entityId, operation, payload, updatedAt, null);
        }
    }

    public record UploadResult(
        Integer acceptedCount,
        Integer failedCount,
        String status,
        String nextCursor,
        List<String> acceptedOperationIds,
        List<String> failedOperationIds,
        List<SyncOperationFailure> failures,
        List<SyncOperationResult> operationResults
    ) {
        public UploadResult(
            Integer acceptedCount,
            Integer failedCount,
            String status,
            String nextCursor,
            List<String> acceptedOperationIds,
            List<String> failedOperationIds,
            List<SyncOperationFailure> failures
        ) {
            this(
                acceptedCount,
                failedCount,
                status,
                nextCursor,
                acceptedOperationIds,
                failedOperationIds,
                failures,
                List.of()
            );
        }
    }

    public record SyncOperationFailure(String operationId, String code, String message) {}

    public record SyncOperationResult(
        String operationId,
        String status,
        String code,
        String message,
        Long serverVersion,
        List<String> conflictFields,
        String serverPayload
    ) {
        public SyncOperationResult(String operationId, String status, String code, String message) {
            this(operationId, status, code, message, null, List.of(), null);
        }
    }

    public record PullResult(List<SyncChange> changes, String effectiveCursor, String nextCursor, Boolean hasMore) {}

    public record CursorStatus(String clientId, String lastCursor, Long updatedAt) {}

    private record PersistedChanges(List<SyncChange> changes, List<Long> sequences) {}


    private static final class SyncConflictException extends RuntimeException {
        private final Long serverVersion;
        private final List<String> conflictFields;
        private final String serverPayload;

        private SyncConflictException(
            String message,
            Long serverVersion,
            List<String> conflictFields,
            String serverPayload
        ) {
            super(message);
            this.serverVersion = serverVersion;
            this.conflictFields = conflictFields == null ? List.of() : List.copyOf(conflictFields);
            this.serverPayload = serverPayload;
        }

        private Long serverVersion() { return serverVersion; }
        private List<String> conflictFields() { return conflictFields; }
        private String serverPayload() { return serverPayload; }
    }

    record CursorToken(long updatedAt, String entityType, String entityId) {
        static CursorToken initial() {
            return new CursorToken(0L, "", "");
        }

        static CursorToken parse(String cursor) {
            if (cursor == null || cursor.isBlank()) {
                return initial();
            }
            int firstSeparator = cursor.indexOf('|');
            if (firstSeparator < 0) {
                return new CursorToken(parseLong(cursor), "", "");
            }
            int secondSeparator = cursor.indexOf('|', firstSeparator + 1);
            if (secondSeparator < 0) {
                return new CursorToken(
                    parseLong(cursor.substring(0, firstSeparator)),
                    cursor.substring(firstSeparator + 1),
                    ""
                );
            }
            return new CursorToken(
                parseLong(cursor.substring(0, firstSeparator)),
                cursor.substring(firstSeparator + 1, secondSeparator),
                cursor.substring(secondSeparator + 1)
            );
        }

        String encode() {
            return updatedAt + "|" + (entityType == null ? "" : entityType) + "|" + (entityId == null ? "" : entityId);
        }

        private static long parseLong(String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
    }
}
