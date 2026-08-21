package com.zhihuiji.backend.domain.entity;

import java.io.Serializable;
import java.util.Objects;

public class SyncOperationLogId implements Serializable {
    private Long ownerUserId;
    private String operationId;

    public SyncOperationLogId() {
    }

    public SyncOperationLogId(Long ownerUserId, String operationId) {
        this.ownerUserId = ownerUserId;
        this.operationId = operationId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SyncOperationLogId that)) {
            return false;
        }
        return Objects.equals(ownerUserId, that.ownerUserId)
            && Objects.equals(operationId, that.operationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerUserId, operationId);
    }
}
