package com.zhihuiji.backend.api.common;

public enum PaymentStatus {
    UNPAID(0),
    PARTIAL(1),
    PAID(2);

    private final int code;

    PaymentStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static boolean isValid(Integer status) {
        return fromCode(status) != null;
    }

    public static PaymentStatus fromCode(Integer status) {
        if (status == null) {
            return null;
        }
        for (PaymentStatus value : values()) {
            if (value.code == status) {
                return value;
            }
        }
        return null;
    }
}
