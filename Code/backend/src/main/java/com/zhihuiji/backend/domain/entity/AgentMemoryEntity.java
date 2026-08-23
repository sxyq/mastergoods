package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Agent 长期记忆实体。
 *
 * <p>按 owner/store 隔离的跨会话事实记忆。来源会话、消息 ID 与敏感级别必须可见，
 * 用户可删除；删除后不能继续被召回。第一版使用数据库文本检索，不引入向量服务。
 *
 * <p>禁止保存凭据、完整认证载荷、私钥、模型密钥、完整手机号或地址等敏感原文；
 * 实体展示名需要脱敏或使用最小展示字段。
 */
@Entity
@Table(name = "agent_memories")
public class AgentMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "source_conversation_id")
    private Long sourceConversationId;

    @Column(name = "source_message_id")
    private Long sourceMessageId;

    @Column(name = "memory_type", nullable = false, length = 64)
    private String memoryType;

    @Column(nullable = false, length = 500)
    private String summary;

    // PostgreSQL TEXT must be bound as a long VARCHAR, not as a locator-backed CLOB.
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "recall_text", columnDefinition = "TEXT")
    private String recallText;

    @Column(nullable = false, length = 32)
    private String sensitivity = "normal";

    @Column(nullable = false)
    private Double confidence = 0.5;

    @Column(nullable = false, length = 32)
    private String status = "active";

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "expires_at")
    private Long expiresAt;

    @Column(name = "last_accessed_at")
    private Long lastAccessedAt;

    public Long getId() { return id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Long getSourceConversationId() { return sourceConversationId; }
    public void setSourceConversationId(Long sourceConversationId) { this.sourceConversationId = sourceConversationId; }
    public Long getSourceMessageId() { return sourceMessageId; }
    public void setSourceMessageId(Long sourceMessageId) { this.sourceMessageId = sourceMessageId; }
    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getRecallText() { return recallText; }
    public void setRecallText(String recallText) { this.recallText = recallText; }
    public String getSensitivity() { return sensitivity; }
    public void setSensitivity(String sensitivity) { this.sensitivity = sensitivity; }
    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
    public Long getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(Long lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
}
