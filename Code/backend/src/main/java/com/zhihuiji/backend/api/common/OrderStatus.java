package com.zhihuiji.backend.api.common;

public enum OrderStatus {
    DRAFT(0),
    COMPLETED(1),
    CANCELLED(2),
    CONFIRMED(3);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static boolean isValid(int status) {
        return fromCode(status) != null;
    }

    public static OrderStatus fromCode(Integer status) {
        if (status == null) {
            return null;
        }
        for (OrderStatus value : values()) {
            if (value.code == status) {
                return value;
            }
        }
        return null;
    }
}
