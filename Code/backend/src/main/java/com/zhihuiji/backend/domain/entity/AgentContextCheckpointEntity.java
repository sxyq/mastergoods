package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Agent 会话上下文检查点（plan 6.7）。
 *
 * <p>检查点是 owner + conversation 隔离的会话级摘要：当历史轮次超过预算时，
 * 由 {@code ContextCompactionService} 选择最早的完整已完成轮次压缩为结构化摘要，
 * 之后的模型请求只读取检查点 + 边界之后的原始消息。
 *
 * <p>持久化字段只保留完成判断所需的最小信息：手机号、地址、凭据、完整认证载荷
 * 和无关客户资料不得进入摘要、SSE 或性能日志；实体显示名必须脱敏。
 *
 * <p>唯一约束 (owner_user_id, conversation_id, source_boundary_message_id,
 * context_policy_version, revision) 防止并发压缩产生两个有效检查点；写入冲突时
 * 由 Repository 层捕获并回退为读取已提交的有效版本。
 */
@Entity
@Table(
    name = "agent_context_checkpoints",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_agent_context_checkpoints_boundary_revision",
        columnNames = {
            "owner_user_id",
            "conversation_id",
            "source_boundary_message_id",
            "context_policy_version",
            "revision"
        }
    )
)
public class AgentContextCheckpointEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "source_boundary_message_id", nullable = false)
    private Long sourceBoundaryMessageId;

    @Column(name = "source_message_count", nullable = false)
    private Integer sourceMessageCount;

    // PostgreSQL TEXT must be bound as LONGVARCHAR (see AgentMessageEntity).
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "summary_body", columnDefinition = "TEXT", nullable = false)
    private String summaryBody;

    @Column(name = "summary_version", nullable = false)
    private Integer summaryVersion = 1;

    @Column(name = "context_policy_version", nullable = false)
    private Integer contextPolicyVersion = 1;

    @Column(name = "tool_schema_version", nullable = false)
    private Integer toolSchemaVersion = 1;

    @Column(nullable = false)
    private Integer revision = 1;

    @Column(nullable = false, length = 32)
    private String quality = "deterministic";

    @Column(nullable = false, length = 32)
    private String status = "active";

    @Column(name = "model_name", length = 128)
    private String modelName;

    @Column(name = "estimated_input_tokens")
    private Integer estimatedInputTokens;

    @Column(name = "estimated_output_tokens")
    private Integer estimatedOutputTokens;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "invalidated_at")
    private Long invalidatedAt;

    @Column(name = "invalidation_reason", length = 128)
    private String invalidationReason;

    public Long getId() { return id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getSourceBoundaryMessageId() { return sourceBoundaryMessageId; }
    public void setSourceBoundaryMessageId(Long sourceBoundaryMessageId) { this.sourceBoundaryMessageId = sourceBoundaryMessageId; }
    public Integer getSourceMessageCount() { return sourceMessageCount; }
    public void setSourceMessageCount(Integer sourceMessageCount) { this.sourceMessageCount = sourceMessageCount; }
    public String getSummaryBody() { return summaryBody; }
    public void setSummaryBody(String summaryBody) { this.summaryBody = summaryBody; }
    public Integer getSummaryVersion() { return summaryVersion; }
    public void setSummaryVersion(Integer summaryVersion) { this.summaryVersion = summaryVersion; }
    public Integer getContextPolicyVersion() { return contextPolicyVersion; }
    public void setContextPolicyVersion(Integer contextPolicyVersion) { this.contextPolicyVersion = contextPolicyVersion; }
    public Integer getToolSchemaVersion() { return toolSchemaVersion; }
    public void setToolSchemaVersion(Integer toolSchemaVersion) { this.toolSchemaVersion = toolSchemaVersion; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public String getQuality() { return quality; }
    public void setQuality(String quality) { this.quality = quality; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getEstimatedInputTokens() { return estimatedInputTokens; }
    public void setEstimatedInputTokens(Integer estimatedInputTokens) { this.estimatedInputTokens = estimatedInputTokens; }
    public Integer getEstimatedOutputTokens() { return estimatedOutputTokens; }
    public void setEstimatedOutputTokens(Integer estimatedOutputTokens) { this.estimatedOutputTokens = estimatedOutputTokens; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    public Long getInvalidatedAt() { return invalidatedAt; }
    public void setInvalidatedAt(Long invalidatedAt) { this.invalidatedAt = invalidatedAt; }
    public String getInvalidationReason() { return invalidationReason; }
    public void setInvalidationReason(String invalidationReason) { this.invalidationReason = invalidationReason; }
}
