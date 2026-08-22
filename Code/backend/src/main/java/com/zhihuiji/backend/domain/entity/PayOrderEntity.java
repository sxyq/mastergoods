package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "pay_orders",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_pay_orders_owner_idempotency",
        columnNames = {"owner_user_id", "idempotency_key"}
    )
)
public class PayOrderEntity {
    @Id
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    private String orderNo;

    @Column(name = "idempotency_key", length = 128)
    private String idempotencyKey;

    @Column(name = "idempotency_payload_hash", length = 64)
    private String idempotencyPayloadHash;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "supplier_name", nullable = false, length = 128)
    private String supplierName;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private Integer method;

    @Column(name = "reference_no", length = 128)
    private String referenceNo;

    @Column(length = 255)
    private String notes;

    @Column(name = "account_id")
    private Long accountId;

    @Column(nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "sync_status", nullable = false)
    private Integer syncStatus;

    @Column(name = "sync_version", nullable = false)
    private Long syncVersion;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyPayloadHash() {
        return idempotencyPayloadHash;
    }

    public void setIdempotencyPayloadHash(String idempotencyPayloadHash) {
        this.idempotencyPayloadHash = idempotencyPayloadHash;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getMethod() {
        return method;
    }

    public void setMethod(Integer method) {
        this.method = method;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(Integer syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Long getSyncVersion() {
        return syncVersion;
    }

    public void setSyncVersion(Long syncVersion) {
        this.syncVersion = syncVersion;
    }
}
