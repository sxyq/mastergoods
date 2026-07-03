package com.zhihuiji.backend.api.common;

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

    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }

    public static PaymentType fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentType value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
