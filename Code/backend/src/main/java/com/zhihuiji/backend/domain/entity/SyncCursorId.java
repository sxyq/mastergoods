package com.zhihuiji.backend.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class SyncCursorId implements Serializable {
    private Long ownerUserId;
    private String clientId;

    public SyncCursorId() {
    }

    public SyncCursorId(Long ownerUserId, String clientId) {
        this.ownerUserId = ownerUserId;
        this.clientId = clientId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SyncCursorId that)) {
            return false;
        }
        return Objects.equals(ownerUserId, that.ownerUserId)
            && Objects.equals(clientId, that.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerUserId, clientId);
    }
}
