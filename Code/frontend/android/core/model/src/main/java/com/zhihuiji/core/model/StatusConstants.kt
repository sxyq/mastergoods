package com.zhihuiji.core.model

object StatusConstants {
    const val STATUS_ACTIVE = 1
    const val STATUS_INACTIVE = 0

    const val SALE_DRAFT = 0
    const val SALE_COMPLETED = 1
    const val SALE_CANCELLED = 2

    const val PAYMENT_UNPAID = 0
    const val PAYMENT_PAID = 1

    const val PAYMENT_TYPE_COLLECT = 1
    const val PAYMENT_TYPE_REFUND = 2

    const val PURCHASE_DRAFT = 0
    const val PURCHASE_RECEIVED = 1

    const val PAY_ORDER_DRAFT = 0
    const val PAY_ORDER_PAID = 1
    const val PAY_ORDER_CANCELLED = 2

    const val FINANCE_INCOME = 1
    const val FINANCE_EXPENSE = 2

    const val FLOW_OUT = 0
    const val FLOW_IN = 1

    const val SOURCE_SALE = 0
    const val SOURCE_ADJUST = 1
}
