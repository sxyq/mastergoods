package com.zhihuiji.backend.api.common;

import java.util.Arrays;

public enum PayOrderStatus {
    DRAFT(0),
    PAID(1),
    CANCELLED(2);

    private final int code;

    PayOrderStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static boolean isValid(Integer status) {
        return fromCode(status) != null;
    }

    public static PayOrderStatus fromCode(Integer status) {
        if (status == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(value -> value.code == status)
            .findFirst()
            .orElse(null);
    }
}
