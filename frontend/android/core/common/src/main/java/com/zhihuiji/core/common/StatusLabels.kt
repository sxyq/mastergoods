package com.zhihuiji.core.common

object StatusLabels {
    object Codes {
        const val SALE_DRAFT = 0
        const val SALE_COMPLETED = 1
        const val SALE_CANCELLED = 2
        const val SALE_CONFIRMED = 3
        const val PURCHASE_DRAFT = 0
        const val PURCHASE_RECEIVED = 1
        const val PAY_PENDING = 0
        const val PAY_PAID = 1
        const val PAY_CANCELLED = 2
        const val FINANCE_INCOME = 1
        const val FINANCE_EXPENSE = 2
        const val ENTITY_ACTIVE = 1
        const val ENTITY_DISABLED = 0
        const val CUSTOMER_STATUS_DISABLED = 0
        const val CUSTOMER_STATUS_ACTIVE = 1
        const val CUSTOMER_NORMAL = 0
        const val CUSTOMER_VIP = 1
        const val CUSTOMER_SVIP = 2
        const val METHOD_CASH = 1
        const val METHOD_WECHAT = 2
        const val METHOD_ALIPAY = 3
        const val METHOD_BANK = 4
        const val METHOD_OTHER = 5
        const val PAYMENT_COLLECT = 1
        const val PAYMENT_REFUND = 2
        const val INVENTORY_OUT = 0
        const val INVENTORY_IN = 1
        const val SALES_RETURN_DRAFT = 0
        const val SALES_RETURN_CONFIRMED = 1
        const val SALES_RETURN_COMPLETED = 2
        const val SALES_RETURN_CANCELLED = 3
    }

    private val SALE_ORDER_STATUS = mapOf(
        Codes.SALE_DRAFT to "草稿", Codes.SALE_COMPLETED to "已完成",
        Codes.SALE_CANCELLED to "已取消", Codes.SALE_CONFIRMED to "已确认",
    )
    fun saleOrderStatus(code: Int): String = SALE_ORDER_STATUS[code] ?: "未知"

    private val PURCHASE_ORDER_STATUS = mapOf(
        Codes.PURCHASE_DRAFT to "草稿", Codes.PURCHASE_RECEIVED to "已收货",
    )
    fun purchaseOrderStatus(code: Int): String = PURCHASE_ORDER_STATUS[code] ?: "未知"

    private val PAY_ORDER_STATUS = mapOf(
        Codes.PAY_PENDING to "待付款", Codes.PAY_PAID to "已付款",
        Codes.PAY_CANCELLED to "已取消",
    )
    fun payOrderStatus(code: Int): String = PAY_ORDER_STATUS[code] ?: "未知"

    private val FINANCE_TYPES = mapOf(
        Codes.FINANCE_INCOME to "收入", Codes.FINANCE_EXPENSE to "支出",
    )
    fun financeType(code: Int): String = FINANCE_TYPES[code] ?: "未知"

    private val SUPPLIER_STATUS = mapOf(
        Codes.ENTITY_ACTIVE to "启用", Codes.ENTITY_DISABLED to "停用",
    )
    fun supplierStatus(code: Int): String = SUPPLIER_STATUS[code] ?: "未知"

    private val CUSTOMER_STATUS = mapOf(
        Codes.CUSTOMER_STATUS_ACTIVE to "正常", Codes.CUSTOMER_STATUS_DISABLED to "已停用",
    )
    fun customerStatus(code: Int): String = CUSTOMER_STATUS[code] ?: "未知"

    fun customerListStatus(code: Int, balance: Double): String = when {
        code == Codes.CUSTOMER_STATUS_DISABLED -> "已停用"
        balance > EPSILON -> "欠款"
        code == Codes.CUSTOMER_STATUS_ACTIVE -> "正常"
        else -> "未知"
    }

    private val PRODUCT_STATUS = mapOf(
        Codes.ENTITY_ACTIVE to "正常", Codes.ENTITY_DISABLED to "停用",
    )
    fun productStatus(code: Int): String = PRODUCT_STATUS[code] ?: "未知"

    private val CUSTOMER_LEVELS = mapOf(
        Codes.CUSTOMER_NORMAL to "普通", Codes.CUSTOMER_VIP to "VIP",
        Codes.CUSTOMER_SVIP to "SVIP",
    )
    fun customerLevel(code: Int): String = CUSTOMER_LEVELS[code] ?: "未知"

    private val PAYMENT_METHODS = mapOf(
        Codes.METHOD_CASH to "现金", Codes.METHOD_WECHAT to "微信",
        Codes.METHOD_ALIPAY to "支付宝", Codes.METHOD_BANK to "银行卡",
        Codes.METHOD_OTHER to "其他",
    )
    fun paymentMethod(code: Int): String = PAYMENT_METHODS[code] ?: "未知"

    private val PAYMENT_TYPES = mapOf(
        Codes.PAYMENT_COLLECT to "收款", Codes.PAYMENT_REFUND to "退款",
    )
    fun paymentType(code: Int): String = PAYMENT_TYPES[code] ?: "未知"

    private val INVENTORY_FLOW_TYPES = mapOf(
        Codes.INVENTORY_OUT to "出库", Codes.INVENTORY_IN to "入库",
    )
    fun inventoryFlowType(code: Int): String = INVENTORY_FLOW_TYPES[code] ?: "未知"

    private val SALES_RETURN_STATUS = mapOf(
        Codes.SALES_RETURN_DRAFT to "草稿",
        Codes.SALES_RETURN_CONFIRMED to "已确认",
        Codes.SALES_RETURN_COMPLETED to "已退款",
        Codes.SALES_RETURN_CANCELLED to "已取消",
    )
    fun salesReturnStatusLabel(code: Int): String = SALES_RETURN_STATUS[code] ?: "未知"

    fun stockStatus(stock: Double, safeStock: Double): String = when {
        stock <= EPSILON -> "缺货"
        stock < safeStock - EPSILON -> "低库存"
        else -> "正常"
    }

    private const val EPSILON = 1e-10
}
