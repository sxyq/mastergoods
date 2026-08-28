package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_scope_grants")
public class AdminScopeGrantEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "admin_account_id", nullable = false) private Long adminAccountId;
    @Column(name = "scope_type", nullable = false, length = 16) private String scopeType;
    @Column(name = "owner_user_id") private Long ownerUserId;
    @Column(name = "store_id") private Long storeId;
    @Column(name = "granted_by") private Long grantedBy;
    @Column(nullable = false) private Integer status;
    @Column(name = "created_at", nullable = false) private Long createdAt;
    public Long getId() { return id; }
    public Long getAdminAccountId() { return adminAccountId; }
    public String getScopeType() { return scopeType; }
    public Long getOwnerUserId() { return ownerUserId; }
    public Long getStoreId() { return storeId; }
    public Long getGrantedBy() { return grantedBy; }
    public Integer getStatus() { return status; }
    public Long getCreatedAt() { return createdAt; }
}
