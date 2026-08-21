package com.zhihuiji.backend.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class SyncTombstoneId implements Serializable {
    private Long ownerUserId;
    private Long storeId;
    private String entityType;
    private String entityId;

    public SyncTombstoneId() {
    }

    public SyncTombstoneId(Long ownerUserId, Long storeId, String entityType, String entityId) {
        this.ownerUserId = ownerUserId;
        this.storeId = storeId;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SyncTombstoneId that)) {
            return false;
        }
        return Objects.equals(ownerUserId, that.ownerUserId)
            && Objects.equals(storeId, that.storeId)
            && Objects.equals(entityType, that.entityType)
            && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerUserId, storeId, entityType, entityId);
    }
}
