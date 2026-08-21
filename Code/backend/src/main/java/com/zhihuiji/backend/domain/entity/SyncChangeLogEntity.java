package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Durable ordered change stream used by sync clients after V27. */
@Entity
@Table(name = "sync_change_log")
public class SyncChangeLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_no", nullable = false)
    private Long sequenceNumber;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "entity_type", length = 64, nullable = false)
    private String entityType;

    @Column(name = "entity_id", length = 64, nullable = false)
    private String entityId;

    @Column(name = "operation", length = 16, nullable = false)
    private String operation;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "sync_version")
    private Long syncVersion;

    @Column(name = "operation_id", length = 128)
    private String operationId;

    @Column(name = "changed_at", nullable = false)
    private Long changedAt;

    public Long getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public Long getSyncVersion() { return syncVersion; }
    public void setSyncVersion(Long syncVersion) { this.syncVersion = syncVersion; }
    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }
    public Long getChangedAt() { return changedAt; }
    public void setChangedAt(Long changedAt) { this.changedAt = changedAt; }
}
