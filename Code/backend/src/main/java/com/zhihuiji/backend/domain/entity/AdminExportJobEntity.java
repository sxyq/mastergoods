package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Short-lived, already-redacted administrator export artifact. */
@Entity
@Table(name = "admin_export_jobs")
public class AdminExportJobEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "export_id", nullable = false, unique = true, length = 128) private String exportId;
    @Column(name = "admin_user_id", nullable = false) private Long adminUserId;
    @Column(name = "export_type", nullable = false, length = 64) private String exportType;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "fields_json", nullable = false, columnDefinition = "TEXT") private String fieldsJson;
    @Column(name = "scope_owner_user_id") private Long scopeOwnerUserId;
    @Column(name = "scope_store_id") private Long scopeStoreId;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "scope_owner_user_ids_json", columnDefinition = "TEXT") private String scopeOwnerUserIdsJson;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "scope_store_ids_json", columnDefinition = "TEXT") private String scopeStoreIdsJson;
    @Column(name = "scope_all_owners") private Boolean scopeAllOwners;
    @Column(name = "scope_all_stores") private Boolean scopeAllStores;
    @Column(name = "requested_from_at") private Long requestedFromAt;
    @Column(name = "requested_to_at") private Long requestedToAt;
    @Column(nullable = false, length = 32) private String status;
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content_csv", columnDefinition = "TEXT") private String contentCsv;
    @Column(name = "error_summary", length = 512) private String errorSummary;
    @Column(name = "idempotency_key", length = 128) private String idempotencyKey;
    @Column(name = "idempotency_payload_hash", length = 64) private String idempotencyPayloadHash;
    @Column(name = "created_at", nullable = false) private Long createdAt;
    @Column(name = "expires_at", nullable = false) private Long expiresAt;
    @Column(name = "completed_at") private Long completedAt;
    @Column(name = "downloaded_at") private Long downloadedAt;
    @Column(name = "download_count", nullable = false) private Integer downloadCount = 0;

    public Long getId() { return id; }
    public String getExportId() { return exportId; }
    public void setExportId(String exportId) { this.exportId = exportId; }
    public Long getAdminUserId() { return adminUserId; }
    public void setAdminUserId(Long adminUserId) { this.adminUserId = adminUserId; }
    public String getExportType() { return exportType; }
    public void setExportType(String exportType) { this.exportType = exportType; }
    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }
    public Long getScopeOwnerUserId() { return scopeOwnerUserId; }
    public void setScopeOwnerUserId(Long scopeOwnerUserId) { this.scopeOwnerUserId = scopeOwnerUserId; }
    public Long getScopeStoreId() { return scopeStoreId; }
    public void setScopeStoreId(Long scopeStoreId) { this.scopeStoreId = scopeStoreId; }
    public String getScopeOwnerUserIdsJson() { return scopeOwnerUserIdsJson; }
    public void setScopeOwnerUserIdsJson(String scopeOwnerUserIdsJson) { this.scopeOwnerUserIdsJson = scopeOwnerUserIdsJson; }
    public String getScopeStoreIdsJson() { return scopeStoreIdsJson; }
    public void setScopeStoreIdsJson(String scopeStoreIdsJson) { this.scopeStoreIdsJson = scopeStoreIdsJson; }
    public Boolean getScopeAllOwners() { return scopeAllOwners; }
    public void setScopeAllOwners(Boolean scopeAllOwners) { this.scopeAllOwners = scopeAllOwners; }
    public Boolean getScopeAllStores() { return scopeAllStores; }
    public void setScopeAllStores(Boolean scopeAllStores) { this.scopeAllStores = scopeAllStores; }
    public Long getRequestedFromAt() { return requestedFromAt; }
    public void setRequestedFromAt(Long requestedFromAt) { this.requestedFromAt = requestedFromAt; }
    public Long getRequestedToAt() { return requestedToAt; }
    public void setRequestedToAt(Long requestedToAt) { this.requestedToAt = requestedToAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getContentCsv() { return contentCsv; }
    public void setContentCsv(String contentCsv) { this.contentCsv = contentCsv; }
    public String getErrorSummary() { return errorSummary; }
    public void setErrorSummary(String errorSummary) { this.errorSummary = errorSummary; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getIdempotencyPayloadHash() { return idempotencyPayloadHash; }
    public void setIdempotencyPayloadHash(String idempotencyPayloadHash) { this.idempotencyPayloadHash = idempotencyPayloadHash; }
    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    public Long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Long expiresAt) { this.expiresAt = expiresAt; }
    public Long getCompletedAt() { return completedAt; }
    public void setCompletedAt(Long completedAt) { this.completedAt = completedAt; }
    public Long getDownloadedAt() { return downloadedAt; }
    public void setDownloadedAt(Long downloadedAt) { this.downloadedAt = downloadedAt; }
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
}
