package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_run_audits")
public class AgentRunAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "run_id", nullable = false, length = 64)
    private String runId;

    @Column(name = "audit_id", nullable = false, length = 128)
    private String auditId;

    @Column(name = "trace_id", nullable = false, length = 128)
    private String traceId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 64)
    private String mode;

    @Column(name = "llm_status", length = 64)
    private String llmStatus;

    @Column(name = "plan_source", length = 64)
    private String planSource;

    @Column(name = "tool_count")
    private Integer toolCount;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Long startedAt;

    @Column(name = "completed_at")
    private Long completedAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "event_count", nullable = false)
    private Integer eventCount;

    @Column(name = "audit_write_dropped_count", nullable = false, columnDefinition = "integer default 0")
    private Integer auditWriteDroppedCount;

    @Column(name = "audit_write_failed_count", nullable = false, columnDefinition = "integer default 0")
    private Integer auditWriteFailedCount;

    @Column(name = "audit_lossy", nullable = false, columnDefinition = "boolean default false")
    private Boolean auditLossy;

    @Column(name = "emitted_event_count", nullable = false, columnDefinition = "integer default 0")
    private Integer emittedEventCount;

    public Long getId() { return id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getAuditId() { return auditId; }
    public void setAuditId(String auditId) { this.auditId = auditId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public String getLlmStatus() { return llmStatus; }
    public void setLlmStatus(String llmStatus) { this.llmStatus = llmStatus; }
    public String getPlanSource() { return planSource; }
    public void setPlanSource(String planSource) { this.planSource = planSource; }
    public Integer getToolCount() { return toolCount; }
    public void setToolCount(Integer toolCount) { this.toolCount = toolCount; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }
    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    public Integer getEventCount() { return eventCount; }
    public void setEventCount(Integer eventCount) { this.eventCount = eventCount; }
    public Integer getAuditWriteDroppedCount() { return auditWriteDroppedCount; }
    public void setAuditWriteDroppedCount(Integer auditWriteDroppedCount) { this.auditWriteDroppedCount = auditWriteDroppedCount; }
    public Integer getAuditWriteFailedCount() { return auditWriteFailedCount; }
    public void setAuditWriteFailedCount(Integer auditWriteFailedCount) { this.auditWriteFailedCount = auditWriteFailedCount; }
    public Boolean getAuditLossy() { return auditLossy; }
    public void setAuditLossy(Boolean auditLossy) { this.auditLossy = auditLossy; }
    public Integer getEmittedEventCount() { return emittedEventCount; }
    public void setEmittedEventCount(Integer emittedEventCount) { this.emittedEventCount = emittedEventCount; }
}
