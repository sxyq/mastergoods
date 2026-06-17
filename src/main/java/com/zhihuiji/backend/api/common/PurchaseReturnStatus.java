package com.zhihuiji.backend.api.common;

import java.util.Arrays;

public enum PurchaseReturnStatus {
    DRAFT(0),
    CONFIRMED(1),
    COMPLETED(2),
    CANCELLED(3);

    private final int code;

    PurchaseReturnStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static boolean isValid(Integer status) {
        return fromCode(status) != null;
    }

    public static PurchaseReturnStatus fromCode(Integer status) {
        if (status == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(value -> value.code == status)
            .findFirst()
            .orElse(null);
    }
}
