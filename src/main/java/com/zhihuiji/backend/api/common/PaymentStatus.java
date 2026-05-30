package com.zhihuiji.backend.api.common;

public final class PaymentStatus {
    private PaymentStatus() {}

    public static final int UNPAID = 0;
    public static final int PARTIAL = 1;
    public static final int PAID = 2;

    public static boolean isValid(int status) {
        return status == UNPAID || status == PARTIAL || status == PAID;
    }
}
