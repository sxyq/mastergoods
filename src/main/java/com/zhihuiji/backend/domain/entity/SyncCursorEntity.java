package com.zhihuiji.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(SyncCursorId.class)
@Table(name = "sync_cursors")
public class SyncCursorEntity {
    @Id
    @Column(name = "owner_user_id", nullable = false)
    private Long ownerUserId;

    @Id
    @Column(name = "client_id", length = 128)
    private String clientId;

    @Column(name = "last_cursor")
    private String lastCursor;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getLastCursor() {
        return lastCursor;
    }

    public void setLastCursor(String lastCursor) {
        this.lastCursor = lastCursor;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
