package com.zhihuiji.backend.api.common;

public final class OrderStatus {
    private OrderStatus() {}

    public static final int DRAFT = 0;
    public static final int COMPLETED = 1;
    public static final int CANCELLED = 2;

    public static boolean isValid(int status) {
        return status == DRAFT || status == COMPLETED || status == CANCELLED;
    }
}
