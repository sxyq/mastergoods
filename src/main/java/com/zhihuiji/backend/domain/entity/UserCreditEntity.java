package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

@Entity
@Table(name = "user_credits")
public class UserCreditEntity {
    @Id
    private Long id;

    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(name = "total_recharged", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRecharged;

    @Column(name = "total_consumed", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalConsumed;

    @Version
    @Column(nullable = false)
    private Integer version;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getTotalRecharged() {
        return totalRecharged;
    }

    public void setTotalRecharged(BigDecimal totalRecharged) {
        this.totalRecharged = totalRecharged;
    }

    public BigDecimal getTotalConsumed() {
        return totalConsumed;
    }

    public void setTotalConsumed(BigDecimal totalConsumed) {
        this.totalConsumed = totalConsumed;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
