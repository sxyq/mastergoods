package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Versioned, secret-free Agent configuration snapshot. */
@Entity
@Table(name = "admin_agent_configs")
public class AdminAgentConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "model_id", length = 128) private String modelId;
    @Column(name = "agent_enabled", nullable = false) private Boolean agentEnabled = false;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "enabled_tools_json", nullable = false, columnDefinition = "TEXT") private String enabledToolsJson = "[]";
    @Column(name = "scope_owner_user_id") private Long scopeOwnerUserId;
    @Column(name = "scope_store_id") private Long scopeStoreId;
    @Column(nullable = false) private Long version = 0L;
    @Column(name = "effective_state", nullable = false, length = 32) private String effectiveState = "PENDING";
    @Column(name = "effective_at") private Long effectiveAt;
    @Column(name = "updated_by") private Long updatedBy;
    @Column(name = "idempotency_key", length = 128) private String idempotencyKey;
    @Column(name = "idempotency_payload_hash", length = 64) private String idempotencyPayloadHash;
    @Column(name = "created_at", nullable = false) private Long createdAt;
    @Column(name = "updated_at", nullable = false) private Long updatedAt;

    public Long getId() { return id; }
    public String getModelId() { return modelId; }
    public void setModelId(String modelId) { this.modelId = modelId; }
    public Boolean getAgentEnabled() { return agentEnabled; }
    public void setAgentEnabled(Boolean agentEnabled) { this.agentEnabled = agentEnabled; }
    public String getEnabledToolsJson() { return enabledToolsJson; }
    public void setEnabledToolsJson(String enabledToolsJson) { this.enabledToolsJson = enabledToolsJson; }
    public Long getScopeOwnerUserId() { return scopeOwnerUserId; }
    public void setScopeOwnerUserId(Long scopeOwnerUserId) { this.scopeOwnerUserId = scopeOwnerUserId; }
    public Long getScopeStoreId() { return scopeStoreId; }
    public void setScopeStoreId(Long scopeStoreId) { this.scopeStoreId = scopeStoreId; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public String getEffectiveState() { return effectiveState; }
    public void setEffectiveState(String effectiveState) { this.effectiveState = effectiveState; }
    public Long getEffectiveAt() { return effectiveAt; }
    public void setEffectiveAt(Long effectiveAt) { this.effectiveAt = effectiveAt; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyPayloadHash() { return idempotencyPayloadHash; }
    public void setIdempotencyPayloadHash(String idempotencyPayloadHash) { this.idempotencyPayloadHash = idempotencyPayloadHash; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}
