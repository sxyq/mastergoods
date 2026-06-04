package com.zhihuiji.backend.api.common;

public final class PartnerTypes {
    public static final String CUSTOMER = "customer";
    public static final String SUPPLIER = "supplier";

    private PartnerTypes() {}

    public static boolean isSupported(String partnerType) {
        return CUSTOMER.equals(partnerType) || SUPPLIER.equals(partnerType);
    }
}
