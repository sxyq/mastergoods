package com.zhihuiji.backend.api.common;

import java.util.Arrays;

public enum PurchaseOrderStatus {
    DRAFT(0),
    RECEIVED(1);

    private final int code;

    PurchaseOrderStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static boolean isValid(Integer status) {
        return fromCode(status) != null;
    }

    public static PurchaseOrderStatus fromCode(Integer status) {
        if (status == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(value -> value.code == status)
            .findFirst()
            .orElse(null);
    }
}
