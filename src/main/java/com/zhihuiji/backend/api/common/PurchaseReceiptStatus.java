package com.zhihuiji.backend.api.common;

import java.util.Arrays;

public enum PurchaseReceiptStatus {
    DRAFT(0),
    CONFIRMED(1),
    CANCELLED(2);

    private final int code;

    PurchaseReceiptStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static boolean isValid(Integer status) {
        return fromCode(status) != null;
    }

    public static PurchaseReceiptStatus fromCode(Integer status) {
        if (status == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(value -> value.code == status)
            .findFirst()
            .orElse(null);
    }
}
