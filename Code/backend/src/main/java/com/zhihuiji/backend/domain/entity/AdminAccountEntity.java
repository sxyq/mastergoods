package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_accounts")
public class AdminAccountEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false, unique = true) private Long userId;
    @Column(name = "role_code", nullable = false, length = 32) private String roleCode;
    @Column(nullable = false) private Integer status;
    @Column(nullable = false) private Long version;
    @Column(name = "created_at", nullable = false) private Long createdAt;
    @Column(name = "updated_at", nullable = false) private Long updatedAt;
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getRoleCode() { return roleCode; }
    public Integer getStatus() { return status; }
    public Long getVersion() { return version; }
    public Long getCreatedAt() { return createdAt; }
    public Long getUpdatedAt() { return updatedAt; }
}
