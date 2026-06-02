package com.zhihuiji.backend.api.common;

import java.util.Arrays;

public enum PaymentType {
    RECEIVE(1),
    REFUND(2);

    private final int code;

    PaymentType(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static PaymentType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        return Arrays.stream(values())
            .filter(value -> value.code == code)
            .findFirst()
            .orElse(null);
    }
}
