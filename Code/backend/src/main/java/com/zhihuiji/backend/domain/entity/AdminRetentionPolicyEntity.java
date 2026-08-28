package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Singleton platform data-retention policy. */
@Entity
@Table(name = "admin_retention_policies")
public class AdminRetentionPolicyEntity {
    @Id private Long id;
    @Column(name = "audit_days", nullable = false) private Integer auditDays;
    @Column(name = "message_days", nullable = false) private Integer messageDays;
    @Column(name = "tool_result_days", nullable = false) private Integer toolResultDays;
    @Column(name = "metrics_days", nullable = false) private Integer metricsDays;
    @Column(name = "content_mode", nullable = false, length = 32) private String contentMode;
    @Column(nullable = false) private Long version;
    @Column(name = "effective_at", nullable = false) private Long effectiveAt;
    @Column(name = "updated_by") private Long updatedBy;
    @Column(name = "idempotency_key", length = 128) private String idempotencyKey;
    @Column(name = "idempotency_payload_hash", length = 64) private String idempotencyPayloadHash;
    @Column(length = 512) private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Integer getAuditDays() { return auditDays; }
    public void setAuditDays(Integer auditDays) { this.auditDays = auditDays; }
    public Integer getMessageDays() { return messageDays; }
    public void setMessageDays(Integer messageDays) { this.messageDays = messageDays; }
    public Integer getToolResultDays() { return toolResultDays; }
    public void setToolResultDays(Integer toolResultDays) { this.toolResultDays = toolResultDays; }
    public Integer getMetricsDays() { return metricsDays; }
    public void setMetricsDays(Integer metricsDays) { this.metricsDays = metricsDays; }
    public String getContentMode() { return contentMode; }
    public void setContentMode(String contentMode) { this.contentMode = contentMode; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Long getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Long effectiveAt) { this.effectiveAt = effectiveAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyPayloadHash() { return idempotencyPayloadHash; }
    public void setIdempotencyPayloadHash(String idempotencyPayloadHash) { this.idempotencyPayloadHash = idempotencyPayloadHash; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
