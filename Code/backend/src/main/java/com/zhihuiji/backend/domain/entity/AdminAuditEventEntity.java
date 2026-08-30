package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Immutable administrator action record. It stores bounded summaries only. */
@Entity
@Table(name = "admin_audit_events")
public class AdminAuditEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_id", nullable = false, unique = true, length = 128) private String eventId;
    // Null identifies an anonymous request; authenticated non-admin attempts retain the user ID.
    @Column(name = "admin_user_id") private Long adminUserId;
    @Column(name = "role_code", nullable = false, length = 32) private String roleCode;
    @Column(nullable = false, length = 64) private String action;
    @Column(name = "resource_type", length = 64) private String resourceType;
    @Column(name = "resource_id", length = 128) private String resourceId;
    @Column(name = "owner_user_id") private Long ownerUserId;
    @Column(name = "store_id") private Long storeId;
    @Column(name = "source_ip", length = 64) private String sourceIp;
    @Column(name = "user_agent_summary", length = 256) private String userAgentSummary;
    @Column(name = "request_id", length = 128) private String requestId;
    @Column(nullable = false, length = 32) private String result;
    @Column(length = 512) private String reason;
    @Column(length = 1000) private String summary;
    @Column(name = "idempotency_key", length = 128) private String idempotencyKey;
    @Column(name = "idempotency_payload_hash", length = 64) private String idempotencyPayloadHash;
    @Column(name = "occurred_at", nullable = false) private Long occurredAt;

    public Long getId() { return id; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public Long getAdminUserId() { return adminUserId; }
    public void setAdminUserId(Long adminUserId) { this.adminUserId = adminUserId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getUserAgentSummary() { return userAgentSummary; }
    public void setUserAgentSummary(String userAgentSummary) { this.userAgentSummary = userAgentSummary; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyPayloadHash() { return idempotencyPayloadHash; }
    public void setIdempotencyPayloadHash(String idempotencyPayloadHash) { this.idempotencyPayloadHash = idempotencyPayloadHash; }
    public Long getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Long occurredAt) { this.occurredAt = occurredAt; }
}
