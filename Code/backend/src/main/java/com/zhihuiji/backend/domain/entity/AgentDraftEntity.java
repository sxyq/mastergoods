package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "agent_drafts")
public class AgentDraftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "conversation_id")
    private Long conversationId;

    @Column(name = "draft_type", nullable = false, length = 32)
    private String draftType;

    @Column(nullable = false, length = 255)
    private String title;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content_json", nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "confirmed_at")
    private Long confirmedAt;

    @Column(name = "business_reference", length = 128)
    private String businessReference;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    public Long getId() { return id; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getDraftType() { return draftType; }
    public void setDraftType(String draftType) { this.draftType = draftType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentJson() { return contentJson; }
    public void setContentJson(String contentJson) { this.contentJson = contentJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
    public Long getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Long confirmedBy) { this.confirmedBy = confirmedBy; }
    public Long getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Long confirmedAt) { this.confirmedAt = confirmedAt; }
    public String getBusinessReference() { return businessReference; }
    public void setBusinessReference(String businessReference) { this.businessReference = businessReference; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
